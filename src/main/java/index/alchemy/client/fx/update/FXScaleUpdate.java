package index.alchemy.client.fx.update;

import index.alchemy.api.ICycle;
import index.alchemy.api.IFXUpdate;
import index.alchemy.client.fx.AlchemyFX;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FXScaleUpdate implements IFXUpdate {
	
	protected ICycle cycle;
	
	public FXScaleUpdate(ICycle cycle) {
		this.cycle = cycle;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void updateFX(AlchemyFX fx, long tick) {
		fx.setScaleF(cycle.next());
	}

}