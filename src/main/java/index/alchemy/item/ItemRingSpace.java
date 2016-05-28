package index.alchemy.item;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import index.alchemy.annotation.KeyEvent;
import index.alchemy.api.Alway;
import index.alchemy.api.ICoolDown;
import index.alchemy.api.IGuiHandle;
import index.alchemy.api.IInputHandle;
import index.alchemy.api.IItemInventory;
import index.alchemy.api.INetworkMessage;
import index.alchemy.client.AlchemyKeyBinding;
import index.alchemy.core.AlchemyModLoader;
import index.alchemy.item.AlchemyItemBauble.AlchemyItemRing;
import index.alchemy.item.ItemRingSpace.MessageSpaceRingPickup;
import index.alchemy.network.AlchemyNetworkHandler;
import index.alchemy.network.SDouble6Package;
import index.alchemy.util.AABBHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRingSpace extends AlchemyItemRing implements IItemInventory, IInputHandle, IGuiHandle, ICoolDown, INetworkMessage.Server<MessageSpaceRingPickup> {
	
	public static final int PICKUP_CD = 20 * 3, SIZE = 9 * 6;
	public static final String NBT_KEY_CD = "cd_ring_space",
			KEY_DESCRIPTION_OPEN = "key.space_ring_open", KEY_DESCRIPTION_PICKUP = "key.space_ring_pickup";
	
	@Override
	public ItemInventory getItemInventory(EntityPlayer player, ItemStack item) {
		return new ItemInventory(player, item, SIZE, I18n.translateToLocal(getInventoryUnlocalizedName()));
	}
	
	@Override
	public String getInventoryUnlocalizedName() {
		return getUnlocalizedName().replace("item.", "inventory.");
	}
	
	@Override
	public void onWornTick(ItemStack item, EntityLivingBase living) {
		if (Alway.isServer() && living instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) living;
			if (player.getHealth() > 0.0F && !player.isSpectator())
				for (Entity entity : player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().expand(5D, 5D, 5D)))
					if (!entity.isDead && entity instanceof EntityItem)
			        	   onCollideWithPlayer((EntityItem) entity, player);
		}
	}
	
	private void onCollideWithPlayer(EntityItem entity, EntityPlayer player) {
		if (entity.delayBeforeCanPickup > 0)
			return;
        ItemStack itemstack = entity.getEntityItem();
        int i = itemstack.stackSize;

        int hook = ForgeEventFactory.onItemPickup(entity, player, itemstack);
        if (hook < 0)
        	return;

        if ((entity.getOwner() == null || entity.lifespan - entity.getAge() <= 200 ||
        		entity.getOwner().equals(player.getName())) && (hook == 1 || i <= 0 || player.inventory.addItemStackToInventory(itemstack))) {
            if (itemstack.getItem() == Item.getItemFromBlock(Blocks.LOG))
            	player.addStat(AchievementList.MINE_WOOD);

            if (itemstack.getItem() == Item.getItemFromBlock(Blocks.LOG2))
            	player.addStat(AchievementList.MINE_WOOD);

            if (itemstack.getItem() == Items.LEATHER)
            	player.addStat(AchievementList.KILL_COW);

            if (itemstack.getItem() == Items.DIAMOND)
            	player.addStat(AchievementList.DIAMONDS);

            if (itemstack.getItem() == Items.BLAZE_ROD)
            	player.addStat(AchievementList.BLAZE_ROD);

            if (itemstack.getItem() == Items.DIAMOND && entity.getThrower() != null) {
                EntityPlayer entityplayer = entity.worldObj.getPlayerEntityByName(entity.getThrower());
                if (entityplayer != null && entityplayer != player)
                	entityplayer.addStat(AchievementList.DIAMONDS_TO_YOU);
            }

            FMLCommonHandler.instance().firePlayerItemPickupEvent(player, entity);
            if (!entity.isSilent())
            	entity.worldObj.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
            			((entity.rand.nextFloat() - entity.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);

            player.onItemPickup(entity, i);

            if (itemstack.stackSize <= 0)
            	entity.setDead();

            player.addStat(StatList.getObjectsPickedUpStats(itemstack.getItem()), i);
        }
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public KeyBinding[] getKeyBindings() {
		AlchemyModLoader.checkState();
		return new KeyBinding[] {
				new AlchemyKeyBinding(KEY_DESCRIPTION_OPEN, Keyboard.KEY_R),
				new AlchemyKeyBinding(KEY_DESCRIPTION_PICKUP, Keyboard.KEY_C)
		};
	}
	
	@SideOnly(Side.CLIENT)
	@KeyEvent(KEY_DESCRIPTION_OPEN)
	public void onKeyOpenPressed(KeyBinding binding) {
		if (isEquipmented(Minecraft.getMinecraft().thePlayer))
			AlchemyNetworkHandler.openGui(this);
	}
	
	@SideOnly(Side.CLIENT)
	@KeyEvent(KEY_DESCRIPTION_PICKUP)
	public void onKeyPickupPressed(KeyBinding binding) {
		if (isCDOver()) {
			AlchemyNetworkHandler.network_wrapper.sendToServer(new MessageSpaceRingPickup());
			restartCD();
		}
	}
	
	public static class MessageSpaceRingPickup implements IMessage {
		@Override
		public void fromBytes(ByteBuf buf) {}

		@Override
		public void toBytes(ByteBuf buf) {}
	}
	
	@Override
	public Class<MessageSpaceRingPickup> getServerMessageClass() {
		return MessageSpaceRingPickup.class;
	}
	
	@Override
	public IMessage onMessage(MessageSpaceRingPickup message, MessageContext ctx) {
		pickup(ctx.getServerHandler().playerEntity);
		return null;
	}
	
	public void pickup(EntityPlayer player) {
		ItemInventory inventory = getItemInventory(player, getFormPlayer(player));
		if (inventory == null)
			return;
		List<EntityItem> list = player.worldObj.getEntitiesWithinAABB(EntityItem.class, AABBHelper.getAABBFromEntity(player, 8D));
		List<SDouble6Package> d6p = new LinkedList<SDouble6Package>(); 
		for (EntityItem entity : list) {
			inventory.mergeItemStack(entity.getEntityItem());
			if (entity.getEntityItem().stackSize < 1) {
				d6p.add(new SDouble6Package(entity.posX, entity.posY, entity.posZ, 0, 0, 0));
				entity.setDead();
			}
		}
		if (list.size() > 0) {
			inventory.updateNBT();
			AlchemyNetworkHandler.spawnParticle(EnumParticleTypes.PORTAL, AABBHelper.getAABBFromEntity(player, 64D), player.worldObj, d6p);
		}
	}
	
	@Override
	public Object getServerGuiElement(EntityPlayer player, World world, int x, int y, int z) {
		ItemInventory inventory = getItemInventory(player, getFormPlayer(player));
		return inventory == null ? null : new ContainerChest(player.inventory, inventory, player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object getClientGuiElement(EntityPlayer player, World world, int x, int y, int z) {
		ItemInventory inventory = getItemInventory(player, getFormPlayer(player));
		return inventory == null ? null : new GuiChest(player.inventory, inventory);
	}
	
	@Override
	public int getMaxCD() {
		return PICKUP_CD;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getResidualCD() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		return isEquipmented(player) ? 
				Math.max(0, getMaxCD() - (player.ticksExisted - player.getEntityData().getInteger(NBT_KEY_CD))) : -1;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isCDOver() {
		return getResidualCD() == 0;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void setResidualCD(int cd) {
		Minecraft.getMinecraft().thePlayer.getEntityData().setInteger(NBT_KEY_CD, Minecraft.getMinecraft().thePlayer.ticksExisted - (getMaxCD() - cd));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void restartCD() {
		Minecraft.getMinecraft().thePlayer.getEntityData().setInteger(NBT_KEY_CD, Minecraft.getMinecraft().thePlayer.ticksExisted);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderID() {
		return 1;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderCD(int x, int y, int w, int h) {}
	
	public ItemRingSpace() {
		super("ring_space", 0xE451F2);
	}
	
}