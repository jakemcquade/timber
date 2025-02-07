package timber.jakemc_dev;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class StateStorage extends PersistentState {
    public HashMap<UUID, PlayerData> players = new HashMap<>();
    public boolean timber_enabled = false;

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putBoolean("timber_enabled", timber_enabled);
        return nbt;
    }

    public static StateStorage createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateStorage state = new StateStorage();
        state.timber_enabled = tag.getBoolean("timber_enabled");

        NbtCompound playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();
            playerData.timber_enabled = playersNbt.getCompound(key).getBoolean("timber_enabled");

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        return state;
    }

    private static Type<StateStorage> type = new Type<>(StateStorage::new, StateStorage::createFromNbt, null);
    public static StateStorage getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        StateStorage state = persistentStateManager.getOrCreate(type, Timber.MOD_ID);
        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        StateStorage serverState = getServerState(player.getWorld().getServer());
        PlayerData playerState = serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());

        return playerState;
    }
}