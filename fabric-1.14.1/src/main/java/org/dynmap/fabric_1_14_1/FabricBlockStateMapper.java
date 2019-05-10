package org.dynmap.fabric_1_14_1;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.registry.Registry;
import org.dynmap.renderer.DynmapBlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FabricBlockStateMapper {
    static final FabricBlockStateMapper INSTANCE = new FabricBlockStateMapper();

    private final Map<BlockState, DynmapBlockState> dynmapBlockStateMap = new HashMap<>();

    private static <T extends Comparable<T>> String getPropertyValue(BlockState state, Property<T> property) {
        return property.getValueAsString(state.get(property));
    }

    private static String propertiesToString(BlockState state) {
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (Property<?> property : state.getProperties()) {
            if ((count++) > 0) {
                result.append(',');
            }
            result.append(property.getName()).append('=').append(getPropertyValue(state, property));
        }

        return result.toString();
    }

    public DynmapBlockState get(BlockState state) {
        return dynmapBlockStateMap.get(state);
    }

    public void init() {
        dynmapBlockStateMap.clear();

        for (Block block : Registry.BLOCK) {
            String blockName = Registry.BLOCK.getId(block).toString();
            List<BlockState> states = block.getStateFactory().getStates();

            // first, add default state
            DynmapBlockState defaultState = null;

            for (int stateIdx = 0; stateIdx < states.size(); stateIdx++) {
                BlockState state = states.get(stateIdx);
                DynmapBlockState dynmapState = new DynmapBlockState(
                        defaultState, stateIdx,
                        blockName, propertiesToString(state),
                        state.getMaterial().toString()
                );

                dynmapBlockStateMap.put(state, dynmapState);
                if (defaultState == null) {
                    defaultState = dynmapState;
                }

                if (state.isAir()) dynmapState.setAir(); else {
                    if (state.getMaterial().isSolid()) dynmapState.setSolid();
                    if (state.getMaterial() == Material.WOOD) dynmapState.setLog();
                    else if (state.getMaterial() == Material.LEAVES) dynmapState.setLeaves();
                }

                if (state.getProperties().contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED)) {
                    dynmapState.setWaterlogged();
                }
            }
        }
    }
}
