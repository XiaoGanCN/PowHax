package powie.powhax.modules;

import powie.powhax.Powhax;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.orbit.EventHandler;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class deathCommands extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> SupportStarscript = sgGeneral.add(new BoolSetting.Builder()
        .name("Support Starscript")
        .description("Makes Starscript work with death commands")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleAfterDeath = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle After Death")
            .description("Turns off the module after its activated.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> StartDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Start Delay")
        .description("Tick delay before running commands")
        .defaultValue(5)
        .min(0)
        .sliderMax(600)
        .build()
    );

    private final Setting<Integer> IntervalDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Interval Delay")
        .description("Tick delay for each command")
        .defaultValue(5)
        .min(0)
        .sliderMax(600)
        .build()
    );

    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
        .name("commands")
        .description("List of commands to be sent.")
        .defaultValue(Arrays.asList(
            "oh no i died :(",
            "/kit default"
        ))
        .visible(() -> !SupportStarscript.get())
        .build()
    );

    private final Setting<List<String>> commandsStarscript = sgGeneral.add(new StringListSetting.Builder()
        .name("commands")
        .description("List of commands to be sent.")
        .defaultValue(Arrays.asList(
            "aw man i died :(",
            "and i respawned at {player.pos}"
        ))
        .renderer(StarscriptTextBoxRenderer.class)
        .visible(SupportStarscript::get)
        .build()
    );

    public deathCommands() {
        super(Powhax.CATEGORY, "death-commands", "Run commands when you die.");
    }

    boolean firstCommand = true;
    boolean running = false;
    int startDelay = 0;
    int intervalDelay = 0;
    private Queue<String> commandQueue = new LinkedList<>();

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket packet) || running) return;

        commandQueue.clear();

        if (SupportStarscript.get()) {
            commandQueue.addAll(commandsStarscript.get());
        } else {
            commandQueue.addAll(commands.get());
        }
        if (commandQueue.isEmpty()){
            error("Theres no commands to run, idiot.");
            toggle();
            return;
        }
        running = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!running) return;
        if (startDelay <= StartDelay.get()) {
            startDelay++;
            return;
        }
        if (intervalDelay <= IntervalDelay.get() && !firstCommand) {
            intervalDelay++;
            return;
        }

        String command = commandQueue.poll();
        if (SupportStarscript.get()) {
            ChatUtils.sendPlayerMsg(MeteorStarscript.run(MeteorStarscript.compile(command)));
        } else {
            ChatUtils.sendPlayerMsg(command);
        }

        firstCommand = false;
        intervalDelay = 0;

        if (!commandQueue.isEmpty()) return;
        if (toggleAfterDeath.get()) toggle();
        firstCommand = true;
        running = false;
        startDelay = 0;
    }
}
