package com.redis.demo.utils;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author hupeng.net@hotmail.com
 */
public class NamedEnumModule extends SimpleModule {
    public NamedEnumModule() {
        super(
            NamedEnumModule.class.getName(),
            new Version(1, 0, 0, null, null, null)
        );
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(NamedEnum.class, NameEnumMixins.class);
        super.setupModule(context);
    }
}

@JsonDeserialize(using = NamedEnumDeserializer.class)
@JsonSerialize(using = NamedEnumSerializer.class)
abstract class NameEnumMixins {

}
