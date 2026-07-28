package com.nobodiiiii.createbiotech.content.buttercat.block;

import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.function.Consumer;

public class ButterCatEngineVisual extends ShaftVisual<ButterCatEngineBlockEntity> implements SimpleDynamicVisual {
    private final RotatingInstance cat;
    private final RotatingInstance bread;
    private final RotatingInstance rope;
    private final RotatingInstance butter;

    private final Quaternionf blockOrientation;

    private PartialModel currentCatModel;
    private PartialModel currentBreadModel;
    private PartialModel currentRopeModel;
    private PartialModel currentButterModel;
    private float lastAttachmentSpeed = Float.NaN;
    private float lastAttachmentRotationOffset = Float.NaN;


    public ButterCatEngineVisual(VisualizationContext context, ButterCatEngineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        Direction facing = blockState.getValue(ButterCatEngineBlock.HORIZONTAL_FACING);
        blockOrientation =  Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing));

        currentCatModel = blockEntity.getCatModel();
        currentBreadModel = blockEntity.getBreadModel();
        currentRopeModel = blockEntity.getRopeModel();
        currentButterModel = blockEntity.getButterModel();
        cat = createAttachmentInstance(currentCatModel);
        bread = createAttachmentInstance(currentBreadModel);
        rope = createAttachmentInstance(currentRopeModel);
        butter = createAttachmentInstance(currentButterModel);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        updateModels();
        updateAttachmentKinetics(false);
    }

    @Override
    public void update(float pt) {
        super.update(pt);
        updateModels();
        updateAttachmentKinetics(true);
    }

    private RotatingInstance createAttachmentInstance(PartialModel model) {
        RotatingInstance instance =
            instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model)).createInstance();
        setupAttachmentInstance(instance);
        return instance;
    }

    private void setupAttachmentInstance(RotatingInstance instance) {
        instance.rotation.set(blockOrientation);
        instance.setPosition(getVisualPosition());
        updateAttachmentKinetics(instance);
        instance.setChanged();
    }

    private void updateAttachmentKinetics(boolean force) {
        float speed = blockEntity.getSpeed();
        float rotationOffset = ButterCatEngineRenderer.getAttachmentRotationOffsetForBe(blockEntity,
            blockEntity.getBlockPos(), rotationAxis());
        if (!force && Mth.equal(lastAttachmentSpeed, speed) && Mth.equal(lastAttachmentRotationOffset, rotationOffset))
            return;

        lastAttachmentSpeed = speed;
        lastAttachmentRotationOffset = rotationOffset;
        updateAttachmentKinetics(cat);
        updateAttachmentKinetics(bread);
        updateAttachmentKinetics(rope);
        updateAttachmentKinetics(butter);
    }

    private void updateAttachmentKinetics(RotatingInstance instance) {
        instance.setRotationAxis(rotationAxis())
            .setRotationalSpeed(blockEntity.getSpeed() * RotatingInstance.SPEED_MULTIPLIER)
            .setRotationOffset(ButterCatEngineRenderer.getAttachmentRotationOffsetForBe(blockEntity,
                blockEntity.getBlockPos(), rotationAxis()))
            .setChanged();
    }
    private void updateModels() {
        PartialModel newCatModel = blockEntity.getCatModel();
        if (newCatModel != currentCatModel) {
            currentCatModel = newCatModel;
            instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(newCatModel)).stealInstance(cat);
            setupAttachmentInstance(cat);
        }

        PartialModel newBreadModel = blockEntity.getBreadModel();
        if (newBreadModel != currentBreadModel) {
            currentBreadModel = newBreadModel;
            instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(newBreadModel)).stealInstance(bread);
            setupAttachmentInstance(bread);
        }

        PartialModel newRopeModel = blockEntity.getRopeModel();
        if (newRopeModel != currentRopeModel) {
            currentRopeModel = newRopeModel;
            instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(newRopeModel)).stealInstance(rope);
            setupAttachmentInstance(rope);
        }

        PartialModel newButterModel = blockEntity.getButterModel();
        if (newButterModel != currentButterModel) {
            currentButterModel = newButterModel;
            instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(newButterModel)).stealInstance(butter);
            setupAttachmentInstance(butter);
        }
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(cat);
        relight(butter);
        relight(bread);
        relight(rope);
    }
    @Override
    protected void _delete() {
        super._delete();
        cat.delete();
        bread.delete();
        rope.delete();
        butter.delete();
    }


    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(cat);
        consumer.accept(butter);
        consumer.accept(bread);
        consumer.accept(rope);
    }

    @Override
    protected Direction.Axis rotationAxis() {
        return super.rotationAxis();
    }
}

