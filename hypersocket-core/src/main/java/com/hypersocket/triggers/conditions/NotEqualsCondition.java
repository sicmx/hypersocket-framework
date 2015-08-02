package com.hypersocket.triggers.conditions;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.triggers.TriggerCondition;
import com.hypersocket.triggers.TriggerResource;
import com.hypersocket.triggers.ValidationException;

public class NotEqualsCondition implements Condition {

	@Override
	public boolean checkCondition(TriggerCondition condition, TriggerResource trigger,
			SystemEvent event) throws ValidationException {
		if (!event.hasAttribute(condition.getAttributeKey())) {
			return false;
		}

		return !event.getAttribute(condition.getAttributeKey()).equals(
				condition.getConditionValue());
	}

	@Override
	public boolean isValueRequired() {
		return true;
	}
}
