# Porting notes: Ponder / Catnip on Minecraft 26.2

Notes for mods that build on Ponder or Catnip and are moving from 1.21.1 to 26.2. Each entry says
what changed, why, and what to write instead.

This file covers Ponder and Catnip only. Create's and Flywheel's own API changes are not in here yet.

---

## Virtual render model data is gone

**Removed:** `net.createmod.ponder.render.VirtualRenderHelper` — on 1.21.1 it held
`VIRTUAL_PROPERTY`, `VIRTUAL_DATA`, `isVirtual(ModelData)` and `blockModel(BlockState)`. (Upstream's
own 26.x branches had already moved the class to `net.createmod.catnip.impl.neoforge.render`, an
internal package, and stopped populating it; this port removes it outright.)

**Replace with:** a test on the level the model is being asked about —
`level instanceof net.createmod.catnip.api.client.level.VirtualBlockGetter`.

### Why

`VIRTUAL_DATA` was never a piece of information in its own right. It was a courier.

On 1.21.1 a model produced geometry through:

```java
List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                         ModelData data, RenderType renderType);
```

That signature has no level and no position, so a model could not tell whether it was being drawn in
a real world (neighbours, block entities, connected textures all available) or on its own for a GUI
icon, a ghost block or a schematic preview. The answer had to be worked out earlier, in

```java
ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData beData);
```

which *does* get a level — and then smuggled forward as a flag in `ModelData`. That is all
`VIRTUAL_PROPERTY` ever was.

The flag was set in two kinds of place:

- **Derived from the level**, in Catnip's model bufferer, in both `bufferModel` and `bufferBlocks`:

  ```java
  ModelData modelData = level.getModelData(pos);
  if (modelData == ModelData.EMPTY && level instanceof VirtualBlockGetter)
      modelData = VirtualRenderHelper.VIRTUAL_DATA;
  ```

- **Passed outright** where there was no level at all — GUI and item helpers in
  `NeoForgeClientHooksHelper`, and callers such as Create's `SchematicannonRenderer`, which draws the
  block a schematicannon has in flight:

  ```java
  Minecraft.getInstance().getBlockRenderer()
      .renderSingleBlock(state, ms, buffer, light, overlay,
          VirtualRenderHelper.VIRTUAL_DATA, null);
  ```

26.2 replaces both `getQuads` and `getModelData` with a single call that carries the level and the
position all the way down:

```java
void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                  RandomSource random, List<BlockStateModelPart> parts);   // NeoForge
```

The question can now be answered where it is asked, so the courier has nothing left to carry.
Flywheel's `BakedModelBuilder` made the same move, taking `level()` and `pos()` where the 1.21.1 one
took `ModelData`.

### Migrating

A model that used to branch on the flag:

```java
// 1.21.1
@Override
public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData beData) {
    if (VirtualRenderHelper.isVirtual(beData))
        return beData;
    return ModelData.builder().with(MY_PROPERTY, gather(world, pos)).build();
}

@Override
public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                                ModelData data, RenderType renderType) {
    if (!VirtualRenderHelper.isVirtual(data))
        return contextualQuads(data, state, side, rand, renderType);
    return super.getQuads(state, side, rand, data, renderType);
}
```

becomes one method:

```java
// 26.2
@Override
public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                         RandomSource random, List<BlockStateModelPart> parts) {
    if (level instanceof VirtualBlockGetter) {
        super.collectParts(level, pos, state, random, parts);   // detached: unconditional geometry
        return;
    }
    contextualParts(level, pos, state, random, parts);          // in a level: consult surroundings
}
```

If you were a *producer* — passing `VIRTUAL_DATA` because you had no level — pass a virtual getter as
the level instead. `EmptyVirtualBlockGetter.FULL_DARK` / `.FULL_BRIGHT` and
`SinglePosVirtualBlockGetter` all live in `net.createmod.catnip.api.client.level` and satisfy the
test above.

### Which overload you call matters

`BlockStateModel` still carries a bare `collectParts(RandomSource, List)`. NeoForge deprecates it in
favour of the level-aware one, and the two are not interchangeable: on a `DelegateBlockStateModel`
the bare overload goes straight to the wrapped template, so **any override you write is silently
skipped** and the caller gets the model's unconditional geometry.

Call the level-aware overload whenever you have a level, or a model that varies with its
surroundings — connected textures, copycats, the bracket a Create shaft wears — will quietly hand
back its fallback shape. Reserve the bare overload for buffering a model genuinely detached from any
block; Catnip's `bufferModel` uses it for exactly that reason, and `bufferBlocks`, which walks real
positions in a level, uses the level-aware one.

### Known users on 1.21.1

For reference when porting, mods that referenced the old helper: Destroy (`MoleculeRenderer`),
Copycats+ (`CopycatModelNeoForge`, `BakedModelWithDataBuilder`, `KineticCopycatRendererImpl`),
Better Contraption Diagram (`InplaceBlockRenderer`), Create Fluid Logistics
(`CopperSchematicannonRenderer`), Kilt (mixin into `BakedModelBuffererImpl`), and Effortless Building
(via a vendored copy of Create's ghost block and GUI element renderers).
