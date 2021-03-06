package net.darkhax.redshark;

import io.netty.channel.ChannelHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleIndexedCodec;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

@Mod(modid = "redshark", name = "Red Shark", version = "@VERSION@", acceptableRemoteVersions = "*", certificateFingerprint = "@FINGERPRINT@")
public class RedShark {

    public static final Logger log = LogManager.getLogger("Red Shark");

    public static final Configuration config = new Configuration(new File("config", "redshark.cfg"));

    public static String[] hexdumpClasses = config.get("General", "Dump Class", new String[]{""}, "List of packet names that should be followed up with a hex dump.").getStringList();

    public static String filename;

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        String[] files = new File(".").list(new PrefixFileFilter("packets.log"));
        if (files != null && files.length == 0) {
            filename = "packets.log";
        } else if (files != null) {
            int max = -1;
            for (String s : files) {
                String[] dots = s.split("\\.");
                if (!dots[dots.length - 1].equals("log")) {
                    int nMax = Integer.parseInt(dots[dots.length - 1]);
                    max = Math.max(max, nMax);
                }
            }
            filename = String.format("packets.log.%d", max + 1);
        }

        final EnumMap<Side, Map<String, FMLEmbeddedChannel>> channels = ReflectionHelper.getPrivateValue(NetworkRegistry.class, NetworkRegistry.INSTANCE, "channels");

        for (final Entry<Side, Map<String, FMLEmbeddedChannel>> sidedChanelInfo : channels.entrySet()) {

            for (final Entry<String, FMLEmbeddedChannel> channel : sidedChanelInfo.getValue().entrySet()) {

                for (final Entry<String, ChannelHandler> handler : channel.getValue().pipeline()) {

                    if (handler.getValue() instanceof SimpleIndexedCodec) {

                        log.info("Inserting listener to {}'s packet channel.", channel.getKey());
                        channel.getValue().pipeline().addFirst(new ModdedChannelListener((SimpleIndexedCodec) handler.getValue()));
                        break;
                    }
                }
            }
        }

        config.save();
    }
}