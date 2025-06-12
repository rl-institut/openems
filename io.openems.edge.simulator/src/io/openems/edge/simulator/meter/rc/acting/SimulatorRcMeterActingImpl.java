package io.openems.edge.simulator.meter.rc.acting;

import java.io.IOException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.RCMeter.Acting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SimulatorRcMeterActingImpl extends AbstractOpenemsComponent
		implements SimulatorRcMeterActing, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SimulatorDatasource datasource;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ComponentManager componentManager;

	private String ioComponentId = null;
	private String ioChannelIdName = null;

	public SimulatorRcMeterActingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SimulatorRcMeterActing.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}

		this.ioComponentId = config.io_component_id();
		this.ioChannelIdName = config.io_channel_id();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_NOT_METERED;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private void updateChannels() {
		Value<Boolean> ioState = null;
		/*
		 * Get state from configured IO component channel first
		 */
		if (this.ioComponentId != null && !this.ioComponentId.isEmpty() && //
				this.ioChannelIdName != null && !this.ioChannelIdName.isEmpty()) {
			try {
				ChannelAddress ioChannelAddress = new ChannelAddress(this.ioComponentId, this.ioChannelIdName);
				BooleanReadChannel inputChannel = this.componentManager.getChannel(ioChannelAddress);
				ioState = inputChannel.value();
				this.channel(SimulatorRcMeterActing.ChannelId.IO_INPUT_STATE).setNextValue(ioState);
			} catch (Exception e) {
				this.channel(SimulatorRcMeterActing.ChannelId.IO_INPUT_STATE).setNextValue(null);
			}
		} else {
			// IO component/channel not configured
			this.channel(SimulatorRcMeterActing.ChannelId.IO_INPUT_STATE).setNextValue(null);
			// ioState remains null
		}

		Integer simulatedActivePower;
		if (ioState.isDefined() && ioState.get()) {
			// load is switched on
			simulatedActivePower = this.datasource.getValue(OpenemsType.INTEGER,
					new ChannelAddress(this.id(), "ActivePower"));
		} else {
			// load is switched off
			simulatedActivePower = 0;
		}

		this.channel(SimulatorRcMeterActing.ChannelId.SIMULATED_ACTIVE_POWER).setNextValue(simulatedActivePower);
		this._setActivePower(simulatedActivePower);

		var simulatedActivePowerByThree = TypeUtils.divide(simulatedActivePower, 3);
		this._setActivePowerL1(simulatedActivePowerByThree);
		this._setActivePowerL2(simulatedActivePowerByThree);
		this._setActivePowerL3(simulatedActivePowerByThree);
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString() + ", IO State: "
				+ (this.channel(SimulatorRcMeterActing.ChannelId.IO_INPUT_STATE).value());
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Consumption
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower);
		} else {
			// Production (or in this context: "Negative Consumption")
			this.calculateProductionEnergy.update(activePower * -1);
			this.calculateConsumptionEnergy.update(0);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
