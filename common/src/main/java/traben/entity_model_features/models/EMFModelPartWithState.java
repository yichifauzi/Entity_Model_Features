package traben.entity_model_features.models;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import traben.entity_model_features.mixin.accessor.ModelPartAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EMFModelPartWithState extends EMFModelPart {
    public final Int2ObjectOpenHashMap<EMFModelState> allKnownStateVariants = new Int2ObjectOpenHashMap<>();
    public int currentModelVariant = 0;
    Map<String, ModelPart> vanillaChildren = new HashMap<>();
    Runnable startOfRenderRunnable = null;
    Animator tryAnimate = new Animator();

    public EMFModelPartWithState(List<Cuboid> cuboids, Map<String, ModelPart> children) {
        super(cuboids, children);
    }

    void receiveOneTimeOnlyRunnable(Runnable run) {
        startOfRenderRunnable = run;
        getChildrenEMF().values().forEach((child) -> {
            if (child instanceof EMFModelPartWithState emf) {
                emf.receiveOneTimeOnlyRunnable(run);
            }
        });
    }


    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {

        if (startOfRenderRunnable != null) {
            startOfRenderRunnable.run();
        }

        if (tryAnimate != null) {
            tryAnimate.run();
        }
        renderWithTextureOverride(matrices, vertices, light, overlay, red, green, blue, alpha);

    }

    EMFModelState getCurrentState() {
        return new EMFModelState(
                getDefaultTransform(),
                ((ModelPartAccessor) this).getCuboids(),
                getChildrenEMF(),
                xScale, yScale, zScale,
                visible, hidden,
                textureOverride, tryAnimate
        );
    }

    EMFModelState getStateOf(ModelPart modelPart) {
        if (modelPart instanceof EMFModelPartWithState emf) {
            return new EMFModelState(
                    modelPart.getDefaultTransform(),
                    ((ModelPartAccessor) modelPart).getCuboids(),
                    emf.getChildrenEMF(),
                    modelPart.xScale, modelPart.yScale, modelPart.zScale,
                    modelPart.visible, modelPart.hidden,
                    emf.textureOverride, emf.tryAnimate
            );
        }
        return new EMFModelState(
                modelPart.getDefaultTransform(),
                ((ModelPartAccessor) modelPart).getCuboids(),
                new HashMap<>(),
                modelPart.xScale, modelPart.yScale, modelPart.zScale,
                modelPart.visible, modelPart.hidden,
                null, new Animator()
        );
    }

    void setFromState(EMFModelState newState) {
        setDefaultTransform(newState.defaultTransform());
        setTransform(getDefaultTransform());
        ((ModelPartAccessor) this).setCuboids(newState.cuboids());

        ((ModelPartAccessor) this).setChildren(newState.variantChildren());

        xScale = newState.xScale();
        yScale = newState.yScale();
        zScale = newState.zScale();
        visible = newState.visible();
        hidden = newState.hidden();
        textureOverride = newState.texture();
        tryAnimate = newState.animation();
    }


    void setFromStateVariant(EMFModelState newState) {

        setTransform(newState.defaultTransform());

        this.xScale = newState.xScale;
        this.yScale = newState.yScale;
        this.zScale = newState.zScale;

        this.visible = newState.visible;
        this.hidden = newState.hidden;

        setDefaultTransform(newState.defaultTransform());

        ((ModelPartAccessor) this).setCuboids(newState.cuboids());
        ((ModelPartAccessor) this).setChildren(newState.variantChildren());

        textureOverride = newState.texture();
        tryAnimate = newState.animation();
    }


    public void setVariantStateTo(int newVariant) {
        if (currentModelVariant != newVariant) {
            setFromStateVariant(allKnownStateVariants.get(newVariant));
            currentModelVariant = newVariant;
            for (ModelPart part :
                    getChildrenEMF().values()) {
                if (part instanceof EMFModelPartWithState p3)
                    p3.setVariantStateTo(newVariant);
            }
        }
    }

    record EMFModelState(
            ModelTransform defaultTransform,
            // ModelTransform currentTransform,
            List<Cuboid> cuboids,
            Map<String, ModelPart> variantChildren,
            float xScale,
            float yScale,
            float zScale,
            boolean visible,
            boolean hidden,
            Identifier texture,
            Animator animation
    ) {

        public static EMFModelState copy(EMFModelState copyFrom) {
            ModelTransform trans = copyFrom.defaultTransform();
            Animator animator = new Animator();
            animator.setAnimation(copyFrom.animation().getAnimation());
            return new EMFModelState(
                    ModelTransform.of(trans.pivotX, trans.pivotY, trans.pivotZ, trans.pitch, trans.yaw, trans.roll),
                    new ArrayList<>(copyFrom.cuboids()),
                    new HashMap<>(copyFrom.variantChildren()),
                    copyFrom.xScale(),
                    copyFrom.yScale(),
                    copyFrom.zScale(),
                    copyFrom.visible(),
                    copyFrom.hidden(),
                    copyFrom.texture(),
                    animator
            );
        }

    }

}
