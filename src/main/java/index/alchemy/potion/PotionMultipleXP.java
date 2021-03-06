package index.alchemy.potion;

import index.alchemy.api.IEventHandle;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PotionMultipleXP extends AlchemyPotion implements IEventHandle {
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void onPlayerPickupXP(PlayerPickupXpEvent event) {
		PotionEffect effect = event.getEntityPlayer().getActivePotionEffect(AlchemyPotionLoader.multiple_xp);
		if (effect != null)
			event.getOrb().xpValue *= effect.getAmplifier() + 1;
	}
	
	public PotionMultipleXP() {
		super("multiple_xp", false, 0x00FFCC);
	}

}