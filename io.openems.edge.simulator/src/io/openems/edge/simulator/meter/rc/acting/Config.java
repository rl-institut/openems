package io.openems.edge.simulator.meter.rc.acting;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Simulator RCMeter Acting", //
        description = "Configures the Simulator RCMeter Acting component.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this component.")
    String id() default "meter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this component; defaults to Component-ID.")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Datasource-ID", description = "ID of the Simulator Datasource.")
    String datasource_id();

    @AttributeDefinition(name = "IO-Component-ID", description = "ID of the 'Simulator IO Digital' component to read from. Leave empty if not used.", required = false)
    String io_component_id() default "";

    @AttributeDefinition(name = "IO-Channel-ID", description = "Channel-ID on the 'Simulator IO Digital' component (e.g., 'Input1', 'Relay1'). Leave empty if not used.", required = false)
    String io_channel_id() default "";

    String webconsole_configurationFactory_nameHint() default "Simulator RCMeter Acting [{id}]";
}
