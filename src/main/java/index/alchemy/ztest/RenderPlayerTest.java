package index.alchemy.ztest;

import index.alchemy.api.IEventHandle;
import index.alchemy.api.annotation.Init;
import index.alchemy.api.annotation.Test;
import index.alchemy.core.AlchemyEventSystem;
import index.alchemy.core.AlchemyEventSystem.EventType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.LoaderState.ModState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Test
@Init(state = ModState.POSTINITIALIZED)
public class RenderPlayerTest implements IEventHandle {
	
	public boolean shouldPassRender = true;
	public float shadowAlpha = -1;
	
	@Override
	public EventType[] getEventType() {
		return AlchemyEventSystem.EVENT_BUS;
	}
	
	@SubscribeEvent
	public void onRenderPlayer(RenderPlayerEvent.Pre event) {
		/*if (shadowAlpha == -1)
			shadowAlpha = Tool.get(Render.class, 3, event.getRenderer());
		if (shouldPassRender) {
			Tool.set(Render.class, 3, event.getRenderer(), 0F);
			event.setCanceled(true);
			Entity entity = event.getEntity();
			IBlockState iblockstate = Blocks.BROWN_MUSHROOM.getDefaultState();
			int x = (int) entity.posX, y = (int) entity.posY, z = (int) entity.posZ;

			World world = entity.worldObj;

	        if (iblockstate.getRenderType() != EnumBlockRenderType.INVISIBLE) {
	            GlStateManager.pushMatrix();
	            GlStateManager.disableLighting();
	            Tessellator tessellator = Tessellator.getInstance();
	            VertexBuffer vertexbuffer = tessellator.getBuffer();

	            vertexbuffer.begin(7, DefaultVertexFormats.BLOCK);
	            BlockPos blockpos = new BlockPos(entity.posX, entity.posY, entity.posZ);
	            //GlStateManager.translate(-x - .5 * mod(x), -y, -z - .5 * mod(z));
	            BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
	            blockrendererdispatcher.getBlockModelRenderer().renderModel(world, blockrendererdispatcher.getModelForState(iblockstate),
	            		iblockstate, blockpos, vertexbuffer,  false, 0);
	            tessellator.draw();

	            GlStateManager.enableLighting();
	            GlStateManager.popMatrix();
	        }
		} else 
			Tool.set(Render.class, 3, event.getRenderer(), shadowAlpha);*/
	}
	
}