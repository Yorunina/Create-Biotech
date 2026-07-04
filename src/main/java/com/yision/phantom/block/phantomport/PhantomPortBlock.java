package com.yision.phantom.block.phantomport;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.foundation.block.IBE;
import com.yision.phantom.registry.AllBlockEntityTypes;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PhantomPortBlock extends HorizontalDirectionalBlock implements IWrenchable, IBE<PhantomPortBlockEntity> {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BooleanProperty.create("open");
	private static final VoxelShape SHAPE_NORTH = Shapes.or(
		box(0, 0, 0, 16, 2, 16),
		box(1, 4, 5, 15, 13, 14),
		box(2, 2, 2, 14, 13, 5),
		box(2, 2, 14, 14, 4, 16),
		box(0, 10, 0, 2, 14, 16),
		box(1, 13, 1, 15, 16, 15),
		box(14, 2, 0, 16, 4, 16),
		box(0, 2, 0, 2, 4, 16),
		box(14, 10, 0, 16, 14, 16)
	);
	private static final VoxelShape SHAPE_EAST = rotateY(SHAPE_NORTH);
	private static final VoxelShape SHAPE_SOUTH = rotateY(SHAPE_EAST);
	private static final VoxelShape SHAPE_WEST = rotateY(SHAPE_SOUTH);

	public PhantomPortBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
			.setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, OPEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!isValidPositionForPlacement(context.getLevel(), context.getClickedPos())) {
			return null;
		}

		BlockState beltState = context.getLevel().getBlockState(context.getClickedPos().below());
		Direction beltFacing = beltState.getValue(BeltBlock.HORIZONTAL_FACING);
		Direction facing = context.getHorizontalDirection().getOpposite();
		if (facing.getAxis() != beltFacing.getAxis()) {
			facing = beltFacing;
		}
		Player player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			facing = facing.getOpposite();
		}
		return defaultBlockState().setValue(FACING, facing)
			.setValue(OPEN, false);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (!isValidPositionForPlacement(level, pos)) {
			return false;
		}
		BlockState beltState = level.getBlockState(pos.below());
		return state.getValue(FACING).getAxis() == beltState.getValue(BeltBlock.HORIZONTAL_FACING).getAxis();
	}

	private boolean isValidPositionForPlacement(LevelReader level, BlockPos pos) {
		BlockState beltState = level.getBlockState(pos.below());
		return AllBlocks.BELT.has(beltState) && beltState.getValue(BeltBlock.SLOPE) == BeltSlope.HORIZONTAL;
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
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		if (!level.isClientSide && fromPos.equals(pos.below()) && !state.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
			return;
		}
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
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
	public Class<PhantomPortBlockEntity> getBlockEntityClass() {
		return PhantomPortBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PhantomPortBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.PHANTOMPORT.get();
	}
}
