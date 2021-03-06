package cofh.thermalfoundation.fluid;

import cofh.core.fluid.BlockFluidInteractive;
import cofh.lib.util.helpers.DamageHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.thermalfoundation.ThermalFoundation;
import cofh.thermalfoundation.entity.monster.EntityBlizz;
import cofh.thermalfoundation.init.TFFluids;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

public class BlockFluidCryotheum extends BlockFluidInteractive {

	Random random = new Random();

	public static final int LEVELS = 5;
	public static final Material materialFluidCryotheum = new MaterialLiquid(MapColor.ICE);

	private static boolean enableSourceFall = true;
	private static boolean effect = true;

	public BlockFluidCryotheum(Fluid fluid) {

		super(fluid, materialFluidCryotheum, "thermalfoundation", "cryotheum");
		setQuantaPerBlock(LEVELS);
		setTickRate(15);

		setHardness(1000F);
		setLightOpacity(1);
		setParticleColor(0.15F, 0.7F, 1.0F);
	}

	public static void config() {

		String category = "Fluid.Cryotheum";
		String comment;

		comment = "If TRUE, Fluid Cryotheum will be worse than lava, except cold.";
		effect = ThermalFoundation.CONFIG.getConfiguration().getBoolean("Effect", category, effect, comment);

		comment = "If TRUE, Fluid Cryotheum Source blocks will gradually fall downwards.";
		enableSourceFall = ThermalFoundation.CONFIG.getConfiguration().getBoolean("Fall", category, enableSourceFall, comment);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {

		entity.extinguish();

		if (!effect) {
			return;
		}
		if (entity.motionY < -0.05 || entity.motionY > 0.05) {
			entity.motionY *= 0.05;
		}
		if (entity.motionZ < -0.05 || entity.motionZ > 0.05) {
			entity.motionZ *= 0.05;
		}
		if (entity.motionX < -0.05 || entity.motionX > 0.05) {
			entity.motionX *= 0.05;
		}
		if (ServerHelper.isClientWorld(world)) {
			return;
		}
		if (world.getTotalWorldTime() % 8 != 0) {
			return;
		}
		if (entity instanceof EntityZombie || entity instanceof EntityCreeper) {
			EntitySnowman snowman = new EntitySnowman(world);
			snowman.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);
			world.spawnEntityInWorld(snowman);

			entity.setDead();
		} else if (entity instanceof EntityBlizz || entity instanceof EntitySnowman) {
			((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.SPEED, 6 * 20, 0));
			((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 6 * 20, 0));
		} else if (entity instanceof EntityBlaze) {
			entity.attackEntityFrom(DamageHelper.cryotheum, 10F);
		} else {
			boolean t = entity.velocityChanged;
			entity.attackEntityFrom(DamageHelper.cryotheum, 2.0F);
			entity.velocityChanged = t;
		}
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {

		return TFFluids.fluidCryotheum.getLuminosity();
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {

		if (effect) {
			checkForInteraction(world, pos);
		}
		if (enableSourceFall && getMetaFromState(state) == 0) {
			BlockPos offsetPos = pos.add(0, densityDir, 0);
			IBlockState offsetState = world.getBlockState(offsetPos);
			int bMeta = offsetState.getBlock().getMetaFromState(offsetState);

			if (offsetState.getBlock() == this && bMeta != 0) {
				world.setBlockState(offsetPos, this.getDefaultState(), 3);
				world.setBlockToAir(pos);
				return;
			}
		}
		super.updateTick(world, pos, state, rand);
	}

	protected void checkForInteraction(World world, BlockPos pos) {

		if (world.getBlockState(pos) != this) {
			return;
		}

		for (EnumFacing face : EnumFacing.VALUES) {
			interactWithBlock(world, pos.offset(face));
		}
		//Corners
		interactWithBlock(world, pos.add(-1, 0, -1));
		interactWithBlock(world, pos.add(-1, 0, 1));
		interactWithBlock(world, pos.add(1, 0, -1));
		interactWithBlock(world, pos.add(1, 0, 1));
	}

	protected void interactWithBlock(World world, BlockPos pos) {

		IBlockState state = world.getBlockState(pos);

		if (state.getBlock().isAir(state, world, pos) || state.getBlock() == this) {
			return;
		}
		//TODO Unify block interactions in parent class.
		if (hasInteraction(state)) {
			world.setBlockState(pos, getInteraction(state), 3);
		} else if (state.isSideSolid(world, pos, EnumFacing.UP) && world.isAirBlock(pos.offset(EnumFacing.UP))) {
			world.setBlockState(pos.offset(EnumFacing.UP), Blocks.SNOW_LAYER.getDefaultState(), 3);
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		this.setRegistryName("fluid_cryotheum");
		GameRegistry.register(this);
		ItemBlock itemBlock = new ItemBlock(this);
		itemBlock.setRegistryName(this.getRegistryName());
		GameRegistry.register(itemBlock);

		config();

		return true;
	}

	@Override
	public boolean initialize() {

		addInteraction(Blocks.GRASS, Blocks.DIRT);
		addInteraction(Blocks.WATER.getDefaultState(), Blocks.ICE);
		addInteraction(Blocks.WATER, Blocks.SNOW);
		addInteraction(Blocks.FLOWING_WATER.getDefaultState(), Blocks.ICE);
		addInteraction(Blocks.FLOWING_WATER, Blocks.SNOW);
		addInteraction(Blocks.LAVA.getDefaultState(), Blocks.OBSIDIAN);
		addInteraction(Blocks.LAVA, Blocks.STONE);
		addInteraction(Blocks.FLOWING_WATER.getDefaultState(), Blocks.OBSIDIAN);
		addInteraction(Blocks.FLOWING_WATER, Blocks.STONE);
		addInteraction(Blocks.LEAVES, Blocks.AIR);
		addInteraction(Blocks.LEAVES2, Blocks.AIR);
		addInteraction(Blocks.TALLGRASS, Blocks.AIR);
		addInteraction(Blocks.FIRE, Blocks.AIR);
		addInteraction(TFFluids.blockFluidGlowstone.getDefaultState(), Blocks.GLOWSTONE);

		return true;
	}

}
