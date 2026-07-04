package com.nobodiiiii.createbiotech.content.cardboardbox;

import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;

public class LargeCardboardBoxItem extends CapturedEntityBoxItem {

	public LargeCardboardBoxItem(Properties properties) {
		super(properties, "item.create_biotech.large_cardboard_box",
			new PackageStyle("cardboard", 12, 12, 23f, false));
	}
}
