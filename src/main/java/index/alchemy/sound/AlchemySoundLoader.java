package index.alchemy.sound;

import index.alchemy.api.annotation.Init;
import index.alchemy.core.AlchemyResourceLocation;
import index.project.version.annotation.Omega;
import net.minecraftforge.fml.common.LoaderState.ModState;

@Omega
@Init(state = ModState.PREINITIALIZED)
public class AlchemySoundLoader {
	
	public static final AlchemySound
			music_sea = new AlchemySound(new AlchemyResourceLocation("music.sea")),
			record_re_awake = new AlchemySound(new AlchemyResourceLocation("record.re-awake"));

}
