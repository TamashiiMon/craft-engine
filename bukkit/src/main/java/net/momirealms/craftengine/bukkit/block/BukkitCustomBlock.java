package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.block.behavior.UnsafeCompositeBlockBehavior;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.SoundUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class BukkitCustomBlock extends AbstractCustomBlock {
    private static final Object ALWAYS_FALSE = FastNMS.INSTANCE.method$StatePredicate$always(false);
    private static final Object ALWAYS_TRUE = FastNMS.INSTANCE.method$StatePredicate$always(true);

    protected BukkitCustomBlock(
            @NotNull Key id,
            @NotNull Holder.Reference<CustomBlock> holder,
            @NotNull Map<String, Property<?>> properties,
            @NotNull Map<String, Integer> appearances,
            @NotNull Map<String, VariantState> variantMapper,
            @NotNull BlockSettings settings,
            @NotNull Map<EventTrigger, List<Function<PlayerOptionalContext>>> events,
            @Nullable List<Map<String, Object>> behavior,
            @Nullable LootTable<?> lootTable
    ) {
        super(id, holder, properties, appearances, variantMapper, settings, events, behavior, lootTable);
    }

    @Override
    protected BlockBehavior setupBehavior(List<Map<String, Object>> behaviorConfig) {
        if (behaviorConfig.isEmpty()) {
            return new EmptyBlockBehavior();
        } else if (behaviorConfig.size() == 1) {
            return BlockBehaviors.fromMap(this, behaviorConfig.get(0));
        } else {
            List<AbstractBlockBehavior> behaviors = new ArrayList<>();
            for (Map<String, Object> config : behaviorConfig) {
                behaviors.add((AbstractBlockBehavior) BlockBehaviors.fromMap(this, config));
            }
            return new UnsafeCompositeBlockBehavior(this, behaviors);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public LootTable<ItemStack> lootTable() {
        return (LootTable<ItemStack>) super.lootTable();
    }

    @Override
    protected void applyPlatformSettings() {
        try {
            for (ImmutableBlockState state : variantProvider().states()) {
                if (state.vanillaBlockState() == null) {
                    CraftEngine.instance().logger().warn("Could not find vanilla block state for " + state + ". This might cause errors!");
                    continue;
                } else if (state.customBlockState() == null) {
                    CraftEngine.instance().logger().warn("Could not find custom block state for " + state + ". This might cause errors!");
                    continue;
                }
                Object mcBlockState = state.customBlockState().handle();
                BlockSettings settings = state.settings();
                // set block state properties
                BlockStateUtils.setInstrument(mcBlockState, settings.instrument());
                BlockStateUtils.setMapColor(mcBlockState, settings.mapColor());
                BlockStateUtils.setLightEmission(mcBlockState, settings.luminance());
                BlockStateUtils.setBurnable(mcBlockState, settings.burnable());
                BlockStateUtils.setHardness(mcBlockState, settings.hardness());
                BlockStateUtils.setPushReaction(mcBlockState, settings.pushReaction());
                BlockStateUtils.setReplaceable(mcBlockState, settings.replaceable());
                boolean canOcclude;
                if (settings.canOcclude() == Tristate.TRUE) {
                    BlockStateUtils.setCanOcclude(mcBlockState, true);
                    canOcclude = true;
                } else if (settings.canOcclude() == Tristate.FALSE) {
                    BlockStateUtils.setCanOcclude(mcBlockState, false);
                    canOcclude = false;
                } else {
                    canOcclude = BlockStateUtils.isOcclude(state.vanillaBlockState().handle());
                    BlockStateUtils.setCanOcclude(mcBlockState, canOcclude);
                }
                boolean useShapeForLightOcclusion;
                if (settings.useShapeForLightOcclusion() == Tristate.TRUE) {
                    BlockStateUtils.setUseShapeForLightOcclusion(mcBlockState, true);
                    useShapeForLightOcclusion = true;
                } else if (settings.useShapeForLightOcclusion() == Tristate.FALSE) {
                    BlockStateUtils.setUseShapeForLightOcclusion(mcBlockState, false);
                    useShapeForLightOcclusion = false;
                } else {
                    useShapeForLightOcclusion = CoreReflections.field$BlockStateBase$useShapeForLightOcclusion.getBoolean(state.vanillaBlockState().handle());
                    BlockStateUtils.setUseShapeForLightOcclusion(mcBlockState, useShapeForLightOcclusion);
                }
                if (settings.isRedstoneConductor() == Tristate.TRUE) {
                    BlockStateUtils.setIsRedstoneConductor(mcBlockState, ALWAYS_TRUE);
                } else if (settings.isRedstoneConductor() == Tristate.FALSE) {
                    BlockStateUtils.setIsRedstoneConductor(mcBlockState, ALWAYS_FALSE);
                }
                if (settings.isSuffocating() == Tristate.TRUE) {
                    BlockStateUtils.setIsSuffocating(mcBlockState, ALWAYS_TRUE);
                } else if (settings.isSuffocating() == Tristate.FALSE) {
                    BlockStateUtils.setIsSuffocating(mcBlockState, ALWAYS_FALSE);
                }
                if (settings.isViewBlocking() == Tristate.TRUE) {
                    BlockStateUtils.setIsViewBlocking(mcBlockState, ALWAYS_TRUE);
                } else if (settings.isViewBlocking() == Tristate.FALSE) {
                    BlockStateUtils.setIsViewBlocking(mcBlockState, ALWAYS_FALSE);
                } else {
                    if (settings.isSuffocating() == Tristate.TRUE) {
                        BlockStateUtils.setIsViewBlocking(mcBlockState, ALWAYS_TRUE);
                    } else if (settings.isSuffocating() == Tristate.FALSE) {
                        BlockStateUtils.setIsViewBlocking(mcBlockState, ALWAYS_FALSE);
                    }
                }
                // set parent block properties
                Object mcBlock = BlockStateUtils.getBlockOwner(mcBlockState);
                // bind shape
                Field shapeField = mcBlock.getClass().getField("shapeHolder");
                @SuppressWarnings("unchecked")
                ObjectHolder<BukkitBlockShape> shapeHolder = (ObjectHolder<BukkitBlockShape>) shapeField.get(mcBlock);
                shapeHolder.bindValue(new BukkitBlockShape(state.vanillaBlockState().handle(), Optional.ofNullable(state.settings().supportShapeBlockState()).map(it -> {
                    try {
                        Object blockState = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(it));
                        if (!BlockStateUtils.isVanillaBlock(blockState)) return null;
                        return blockState;
                    } catch (IllegalArgumentException e) {
                        CraftEngine.instance().logger().warn("Illegal shape block state: " + it, e);
                        return null;
                    }
                }).orElse(null)));
                // bind behavior
                Field behaviorField = mcBlock.getClass().getField("behaviorHolder");
                @SuppressWarnings("unchecked")
                ObjectHolder<BlockBehavior> behaviorHolder = (ObjectHolder<BlockBehavior>) behaviorField.get(mcBlock);
                behaviorHolder.bindValue(super.behavior);
                // set block side properties
                CoreReflections.field$BlockBehaviour$explosionResistance.set(mcBlock, settings.resistance());
                CoreReflections.field$BlockBehaviour$soundType.set(mcBlock, SoundUtils.toSoundType(settings.sounds()));
                // 1.21.2以前要在init cache之前设定 isConditionallyFullOpaque
                if (!VersionHelper.isOrAbove1_21_2()) {
                    boolean isConditionallyFullOpaque = canOcclude & useShapeForLightOcclusion;
                    CoreReflections.field$BlockStateBase$isConditionallyFullOpaque.set(mcBlockState, isConditionallyFullOpaque);
                }
                // init cache
                CoreReflections.method$BlockStateBase$initCache.invoke(mcBlockState);
                // modify cache
                if (VersionHelper.isOrAbove1_21_2()) {
                    int blockLight = settings.blockLight() != -1 ? settings.blockLight() : CoreReflections.field$BlockStateBase$lightBlock.getInt(state.vanillaBlockState().handle());
                    // set block light
                    CoreReflections.field$BlockStateBase$lightBlock.set(mcBlockState, blockLight);
                    // set propagates skylight
                    if (settings.propagatesSkylightDown() == Tristate.TRUE) {
                        CoreReflections.field$BlockStateBase$propagatesSkylightDown.set(mcBlockState, true);
                    } else if (settings.propagatesSkylightDown() == Tristate.FALSE) {
                        CoreReflections.field$BlockStateBase$propagatesSkylightDown.set(mcBlockState, false);
                    } else {
                        CoreReflections.field$BlockStateBase$propagatesSkylightDown.set(mcBlockState, CoreReflections.field$BlockStateBase$propagatesSkylightDown.getBoolean(state.vanillaBlockState().handle()));
                    }
                } else {
                    Object cache = CoreReflections.field$BlockStateBase$cache.get(mcBlockState);
                    int blockLight = settings.blockLight() != -1 ? settings.blockLight() : CoreReflections.field$BlockStateBase$Cache$lightBlock.getInt(CoreReflections.field$BlockStateBase$cache.get(state.vanillaBlockState().handle()));
                    // set block light
                    CoreReflections.field$BlockStateBase$Cache$lightBlock.set(cache, blockLight);
                    // set propagates skylight
                    if (settings.propagatesSkylightDown() == Tristate.TRUE) {
                        CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.set(cache, true);
                    } else if (settings.propagatesSkylightDown() == Tristate.FALSE) {
                        CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.set(cache, false);
                    } else {
                        CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.set(cache, CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.getBoolean(CoreReflections.field$BlockStateBase$cache.get(state.vanillaBlockState().handle())));
                    }
                }
                // set fluid later
                if (settings.fluidState()) {
                    CoreReflections.field$BlockStateBase$fluidState.set(mcBlockState, CoreReflections.method$FlowingFluid$getSource.invoke(MFluids.WATER, false));
                } else {
                    CoreReflections.field$BlockStateBase$fluidState.set(mcBlockState, MFluids.EMPTY$defaultState);
                }
                // set random tick later
                BlockStateUtils.setIsRandomlyTicking(mcBlockState, settings.isRandomlyTicking());
                // bind tags
                Object holder = BukkitCraftEngine.instance().blockManager().getMinecraftBlockHolder(state.customBlockState().registryId());
                Set<Object> tags = new HashSet<>();
                for (Key tag : settings.tags()) {
                    tags.add(CoreReflections.method$TagKey$create.invoke(null, MRegistries.BLOCK, KeyUtils.toResourceLocation(tag)));
                }
                CoreReflections.field$Holder$Reference$tags.set(holder, tags);
                // set burning properties
                if (settings.burnable()) {
                    CoreReflections.method$FireBlock$setFlammable.invoke(MBlocks.FIRE, mcBlock, settings.burnChance(), settings.fireSpreadChance());
                }
                CoreReflections.field$BlockStateBase$requiresCorrectToolForDrops.set(mcBlockState, settings.requireCorrectTool());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to init block settings", e);
        }
    }

    public static Builder builder(Key id) {
        return new BuilderImpl(id);
    }

    public static class BuilderImpl implements Builder {
        protected final Key id;
        protected Map<String, Property<?>> properties;
        protected Map<String, Integer> appearances;
        protected Map<String, VariantState> variantMapper;
        protected BlockSettings settings;
        protected List<Map<String, Object>> behavior;
        protected LootTable<?> lootTable;
        protected Map<EventTrigger, List<Function<PlayerOptionalContext>>> events;

        public BuilderImpl(Key id) {
            this.id = id;
        }

        @Override
        public Builder events(Map<EventTrigger, List<Function<PlayerOptionalContext>>> events) {
            this.events = events;
            return this;
        }

        @Override
        public Builder appearances(Map<String, Integer> appearances) {
            this.appearances = appearances;
            return this;
        }

        @Override
        public Builder behavior(List<Map<String, Object>> behavior) {
            this.behavior = behavior;
            return this;
        }

        @Override
        public Builder lootTable(LootTable<?> lootTable) {
            this.lootTable = lootTable;
            return this;
        }

        @Override
        public Builder properties(Map<String, Property<?>> properties) {
            this.properties = properties;
            return this;
        }

        @Override
        public Builder settings(BlockSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Builder variantMapper(Map<String, VariantState> variantMapper) {
            this.variantMapper = variantMapper;
            return this;
        }

        @Override
        public @NotNull CustomBlock build() {
            // create or get block holder
            Holder.Reference<CustomBlock> holder = BuiltInRegistries.BLOCK.get(id).orElseGet(() ->
                    ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(new ResourceKey<>(BuiltInRegistries.BLOCK.key().location(), id)));
            return new BukkitCustomBlock(id, holder, properties, appearances, variantMapper, settings, events, behavior, lootTable);
        }
    }
}
