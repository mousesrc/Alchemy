package index.alchemy.core;

import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public interface IIndexRunnable {
	
	public boolean run(int index, Phase phase);

}