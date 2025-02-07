package timber.jakemc_dev;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Timber implements ModInitializer {
	public static final String MOD_ID = "timber";
	public static final GameRules.Key<GameRules.BooleanRule> TIMBER_ENABLED = GameRuleRegistry.register(
		"timber_enabled",
		GameRules.Category.PLAYER,
		GameRuleFactory.createBooleanRule(true)
	);

	public static void setTimberEnabled(PlayerEntity player, boolean enabled) {
		PlayerData playerState = StateStorage.getPlayerState(player);
		playerState.timber_enabled = enabled;

		player.sendMessage(Text.translatable(enabled ? "§aTimber Enabled" : "§cTimber Disabled"), true);
	}

	public static boolean isTimberEnabled(PlayerEntity player) {
		PlayerData playerState = StateStorage.getPlayerState(player);
		return playerState.timber_enabled;
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
			dispatcher.register(CommandManager.literal("timber").then(
					CommandManager.literal("toggle")
							.requires(serverCommandSource -> serverCommandSource.getWorld().getGameRules().getBoolean(TIMBER_ENABLED))
							.executes(context -> {
								final PlayerEntity player = context.getSource().getPlayerOrThrow();
								final ItemStack stack = player.getMainHandStack();
								if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof AxeItem))
									throw new SimpleCommandExceptionType(
											Text.of("You must hold the axe in your main hand.")).create();

								setTimberEnabled(player, !isTimberEnabled(player));
								return 1;
							})
			));
		});

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			ItemStack axe = player.getMainHandStack();
			if (axe.getItem() instanceof AxeItem && state.isIn(BlockTags.LOGS) && isTimberEnabled(player)) {
				Queue<BlockPos> queue = new LinkedList<>();
				Set<BlockPos> visited = new HashSet<>();
				visited.add(pos);

				for (Direction direction : Direction.values()) {
					BlockPos neighborPos = pos.offset(direction);
					BlockState neighborState = world.getBlockState(neighborPos);

					if (neighborState.isIn(BlockTags.LOGS) && !visited.contains(neighborPos)) {
						queue.add(neighborPos);
						visited.add(neighborPos);
					}
				}

				while (!queue.isEmpty()) {
					BlockPos currentPos = queue.poll();
					BlockState currentState = world.getBlockState(currentPos);

					if (currentState.isIn(BlockTags.LOGS)) {
						world.breakBlock(currentPos, true);

						if (!player.isCreative()) {
							if (axe.getDamage() < axe.getMaxDamage()) {
								axe.setDamage(axe.getDamage() + 1);
							} else break;
						}

						for (Direction direction : Direction.values()) {
							BlockPos neighborPos = currentPos.offset(direction);
							if (!visited.contains(neighborPos) && world.getBlockState(neighborPos).isIn(BlockTags.LOGS)) {
								queue.add(neighborPos);
								visited.add(neighborPos);
							}
						}
					}
				}
			}
		});
	}
}