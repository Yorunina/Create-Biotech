package com.yision.allay.block.allayport;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.yision.allay.registry.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class AllayPortBlock extends HorizontalDirectionalBlock implements IWrenchable, IBE<AllayPortBlockEntity> {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE_NORTH = box(0, 0, 0, 16, 15, 14);
	private static final VoxelShape SHAPE_EAST = rotateY(SHAPE_NORTH);
	private static final VoxelShape SHAPE_SOUTH = rotateY(SHAPE_EAST);
	private static final VoxelShape SHAPE_WEST = rotateY(SHAPE_SOUTH);

	public AllayPortBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
		@NotNull Player player, @NotNull InteractionHand hand,
		@NotNull BlockHitResult hitResult) {
		return onBlockEntityUse(level, pos, blockEntity -> blockEntity.use(player));
	}

	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
		@NotNull CollisionContext context) {
		return getShapeForFacing(state.getValue(FACING));
	}

	@Override
	public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
		@NotNull BlockPos pos, @NotNull CollisionContext context) {
		return getShapeForFacing(state.getValue(FACING));
	}

	private static VoxelShape getShapeForFacing(Direction facing) {
		return switch (facing) {
			case EAST -> SHAPE_EAST;
			case SOUTH -> SHAPE_SOUTH;
			case WEST -> SHAPE_WEST;
			default -> SHAPE_NORTH;
		};
	}

	private static VoxelShape rotateY(VoxelShape shape) {
		VoxelShape[] rotated = new VoxelShape[] { Shapes.empty() };
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> rotated[0] = Shapes.or(rotated[0],
			box(16 - maxZ * 16, minY * 16, minX * 16, 16 - minZ * 16, maxY * 16, maxX * 16)));
		return rotated[0];
	}

	@Override
	public @NotNull InteractionResult onWrenched(BlockState state, UseOnContext context) {
		BlockState rotated = state.setValue(FACING, state.getValue(FACING).getOpposite());
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!rotated.canSurvive(level, pos)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		level.setBlock(pos, rotated, 3);
		IWrenchable.playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			IBE.onRemove(state, level, pos, newState);
			return;
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public Class<AllayPortBlockEntity> getBlockEntityClass() {
		return AllayPortBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends AllayPortBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ALLAY_PORT.get();
	}
}
