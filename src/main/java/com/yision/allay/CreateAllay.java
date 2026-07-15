package com.yision.allay;

import com.mojang.logging.LogUtils;
import com.nobodiiiii.createbiotech.CreateBiotech;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class CreateAllay {
	public static final String MODID = CreateBiotech.MOD_ID;
	public static final String NAME = "Create: Biotech Allay";
	public static final Logger LOGGER = LogUtils.getLogger();

	private CreateAllay() {}

	public static ResourceLocation asResource(String path) {
		return CreateBiotech.asResource(path);
	}
}
