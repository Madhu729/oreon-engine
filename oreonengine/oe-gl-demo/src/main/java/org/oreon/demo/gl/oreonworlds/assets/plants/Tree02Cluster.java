package org.oreon.demo.gl.oreonworlds.assets.plants;

import java.nio.FloatBuffer;
import java.util.List;

import org.oreon.core.gl.buffers.GLMeshVBO;
import org.oreon.core.gl.buffers.GLUBO;
import org.oreon.core.instancing.InstancedDataObject;
import org.oreon.core.instancing.InstancingCluster;
import org.oreon.core.math.Matrix4f;
import org.oreon.core.math.Vec3f;
import org.oreon.core.renderer.Renderer;
import org.oreon.core.scene.GameObject;
import org.oreon.core.scene.Node;
import org.oreon.core.system.CoreSystem;
import org.oreon.core.util.BufferUtil;
import org.oreon.core.util.Constants;
import org.oreon.core.util.IntegerReference;
import org.oreon.demo.gl.oreonworlds.shaders.InstancingGridShader;
import org.oreon.demo.gl.oreonworlds.shaders.assets.plants.TreeBillboardShader;
import org.oreon.demo.gl.oreonworlds.shaders.assets.plants.TreeLeavesShader;
import org.oreon.demo.gl.oreonworlds.shaders.assets.plants.TreeTrunkShader;

public class Tree02Cluster extends InstancingCluster{

	public Tree02Cluster(int instances, Vec3f pos, List<InstancedDataObject> objects){
		
		setCenter(pos);
		setHighPolyInstances(new IntegerReference(0));
		setLowPolyInstances(new IntegerReference(instances));
		int buffersize = Float.BYTES * 16 * instances;
		
		for (int i=0; i<instances; i++){
			
			float s = (float)(Math.random()*6 + 26);
			Vec3f translation = new Vec3f((float)(Math.random()*100)-50 + getCenter().getX(), 0, (float)(Math.random()*100)-50 + getCenter().getZ());
			Vec3f scaling = new Vec3f(s,s,s);
			Vec3f rotation = new Vec3f(0,(float) Math.random()*360f,0);
			
			float terrainHeight = CoreSystem.getInstance().getScenegraph().getTerrain().getTerrainHeight(translation.getX(),translation.getZ());
			terrainHeight -= 1;
			translation.setY(terrainHeight);
			
			Matrix4f translationMatrix = new Matrix4f().Translation(translation);
			Matrix4f rotationMatrix = new Matrix4f().Rotation(rotation);
			Matrix4f scalingMatrix = new Matrix4f().Scaling(scaling);
			
			getWorldMatrices().add(translationMatrix.mul(scalingMatrix.mul(rotationMatrix)));
			getModelMatrices().add(rotationMatrix);
			getLowPolyIndices().add(i);
		}
		
		setModelMatricesBuffer(new GLUBO());
		getModelMatricesBuffer().allocate(buffersize);
		
		setWorldMatricesBuffer(new GLUBO());
		getWorldMatricesBuffer().allocate(buffersize);	
		
		/**
		 * init matrices UBO's
		 */
		int size = Float.BYTES * 16 * instances;
		
		FloatBuffer worldMatricesFloatBuffer = BufferUtil.createFloatBuffer(size);
		FloatBuffer modelMatricesFloatBuffer = BufferUtil.createFloatBuffer(size);
		
		for(Matrix4f matrix : getWorldMatrices()){
			worldMatricesFloatBuffer.put(BufferUtil.createFlippedBuffer(matrix));
		}
		for(Matrix4f matrix : getModelMatrices()){
			modelMatricesFloatBuffer.put(BufferUtil.createFlippedBuffer(matrix));
		}
		
		getWorldMatricesBuffer().updateData(worldMatricesFloatBuffer, size);
		getModelMatricesBuffer().updateData(modelMatricesFloatBuffer, size);
		
		for (InstancedDataObject dataObject : objects){
			GameObject object = new GameObject();
			GLMeshVBO vbo = new GLMeshVBO((GLMeshVBO) dataObject.getVbo());
			vbo.setInstances(new IntegerReference(instances));
			
			Renderer renderer = new Renderer(vbo);
			renderer.setRenderInfo(dataObject.getRenderInfo());
			
			Renderer shadowRenderer = new Renderer(vbo);
			shadowRenderer.setRenderInfo(dataObject.getShadowRenderInfo());
			
			object.addComponent("Material", dataObject.getMaterial());
			object.addComponent(Constants.RENDERER_COMPONENT, renderer);
			object.addComponent(Constants.SHADOW_RENDERER_COMPONENT, shadowRenderer);

			addChild(object);
		}
		
		((GLMeshVBO) ((Renderer) ((GameObject) getChildren().get(0)).getComponent("Renderer")).getVbo()).setInstances(getHighPolyInstances());
		((GLMeshVBO) ((Renderer) ((GameObject) getChildren().get(1)).getComponent("Renderer")).getVbo()).setInstances(getHighPolyInstances());
		
		((GLMeshVBO) ((Renderer) ((GameObject) getChildren().get(2)).getComponent("Renderer")).getVbo()).setInstances(getLowPolyInstances());
	}
	
	@Override
	public void updateUBOs(){
		
		getHighPolyIndices().clear();
		
		int index = 0;
		
		for (Matrix4f transform : getWorldMatrices()){
			if (transform.getTranslation().sub(CoreSystem.getInstance().getScenegraph().getCamera().getPosition()).length() < 220){
				getHighPolyIndices().add(index);
			}

			index++;
		}
		getHighPolyInstances().setValue(getHighPolyIndices().size());
	}

	public void update()
	{	
		super.update();
		
		if (CoreSystem.getInstance().getRenderEngine().isGrid()){
			for (Node child : getChildren()){
				((Renderer) ((GameObject) child).getComponent("Renderer")).getRenderInfo().setShader(InstancingGridShader.getInstance());
			}
		}
		else{
			((Renderer) ((GameObject) getChildren().get(0)).getComponent("Renderer")).getRenderInfo().setShader(TreeTrunkShader.getInstance());
			((Renderer) ((GameObject) getChildren().get(1)).getComponent("Renderer")).getRenderInfo().setShader(TreeLeavesShader.getInstance());
			((Renderer) ((GameObject) getChildren().get(2)).getComponent("Renderer")).getRenderInfo().setShader(TreeBillboardShader.getInstance());
		}
	}
	
	public void renderShadows(){
		
		getHighPolyInstances().setValue(0);
	
		super.renderShadows();
	
		getHighPolyInstances().setValue(getHighPolyIndices().size());
	}
	
	public void render(){
		
//		if (RenderingEngine.isWaterReflection()){
//			((MeshVAO) ((Renderer) ((GameObject) getChildren().get(0)).getComponent("Renderer")).getVao()).setInstances(0);
//			((MeshVAO) ((Renderer) ((GameObject) getChildren().get(1)).getComponent("Renderer")).getVao()).setInstances(0);
//		
//			((MeshVAO) ((Renderer) ((GameObject) getChildren().get(2)).getComponent("Renderer")).getVao()).setInstances(getLowPolyIndices().size());
//		
//			super.render();
//		
//			((MeshVAO) ((Renderer) ((GameObject) getChildren().get(0)).getComponent("Renderer")).getVao()).setInstances(getHighPolyIndices().size());
//			((MeshVAO) ((Renderer) ((GameObject) getChildren().get(1)).getComponent("Renderer")).getVao()).setInstances(getHighPolyIndices().size());
//		}
//		else
			super.render();
	}
}
