package com.yusys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.event.ActivitiActivityEvent;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivitiGlobalEventListener implements ActivitiEventListener {

	protected Logger logger = LoggerFactory.getLogger(ActivitiGlobalEventListener.class);

	private static ActivitiGlobalEventListener instance;
	
	/**
	 * 当流程结束时，如果有上层机构，是否自动启动上层机构的处理流程
	 */
	private boolean startUpperOnFinish;

	public ActivitiGlobalEventListener() {
		instance = this;
	}

	/**
	 * 根据任务角色获取执行候选人ID
	 * 
	 * @return 执行候选人ID列表；没有找到时抛出异常
	 */
	private List<String> calCandidateUser(String logicSysNo, String orgNo, String roleId) {
		List<String> list = new ArrayList<String>(1);
		if ("leaders1".equals(roleId)) {
			list.add("newLeader1");
		} else if ("leaders2".equals(roleId)) {
			list.add("newLeader2");
		}
		if (list.size() == 0) {
			throw new ActivitiObjectNotFoundException("无法找到任务执行候选人");
		}
		return list;
	}

	/**
	 * 对 TASK_CREATED 事件响应，设置当前任务的执行候选人
	 */
	private void taskCreate(ActivitiEvent event) {
		ActivitiEntityEvent entityEvent = (ActivitiEntityEvent)event;
		DelegateTask delegateTask = (DelegateTask)entityEvent.getEntity();
		String logicSysNo = (String)delegateTask.getVariables().get("logicSysNo");
		String orgNo = (String)delegateTask.getVariables().get("orgNo");
		logger.debug("\tlogicSysNo = " + logicSysNo);
		logger.debug("\torgNo = " + orgNo);

		Set<IdentityLink> identityLinkSet = delegateTask.getCandidates();
		// 检查是否已经有执行候选人了
		for (Iterator<IdentityLink> it = identityLinkSet.iterator(); it.hasNext(); ) {
			IdentityLink identityLink = it.next();
			if (! StringUtils.isEmpty(identityLink.getUserId())) {
				// 如果已经有执行候选人了，不再处理
				return;
			}
		}
		Set<String> userIdSet = new HashSet<String>();
		for (Iterator<IdentityLink> it = identityLinkSet.iterator(); it.hasNext(); ) {
			IdentityLink identityLink = it.next();
			// 找到执行候选角色
			if (StringUtils.isEmpty(identityLink.getGroupId())) {
				continue;
			}
			userIdSet.addAll(calCandidateUser(logicSysNo, orgNo, identityLink.getGroupId()));
		}
		// 添加执行候选人
		delegateTask.addCandidateUsers(userIdSet);
	}

	@Override
	public void onEvent(ActivitiEvent event) {
		ActivitiActivityEvent activityEvent;
		ActivitiEntityEvent entityEvent;
		DelegateTask delegateTask;

		switch (event.getType()) {
		case TASK_CREATED:
			logger.debug("Event: TASK_CREATED");
			entityEvent = (ActivitiEntityEvent)event;
			delegateTask = (DelegateTask)entityEvent.getEntity();
			logger.debug("\ttaskId = " + delegateTask.getId());
			taskCreate(event);
			break;
		case TASK_COMPLETED:
			logger.debug("Event: TASK_COMPLETED");
			entityEvent = (ActivitiEntityEvent)event;
			delegateTask = (DelegateTask)entityEvent.getEntity();
			logger.debug("\ttaskId = " + delegateTask.getId());
			break;
		case TASK_ASSIGNED:
			logger.debug("Event: TASK_ASSIGNED");
			entityEvent = (ActivitiEntityEvent)event;
			delegateTask = (DelegateTask)entityEvent.getEntity();
			logger.debug("\ttaskId = " + delegateTask.getId());
			break;
		case ACTIVITY_STARTED:
			logger.debug("Event: ACTIVITY_STARTED");
			activityEvent = (ActivitiActivityEvent)event;
			logger.debug("\tactivityId = " + activityEvent.getActivityId());
			logger.debug("\tactivityName = " + activityEvent.getActivityName());
			logger.debug("\tactivityType = " + activityEvent.getActivityType());
			break;
		case ACTIVITY_COMPLETED:
			logger.debug("Event: ACTIVITY_COMPLETED");
			activityEvent = (ActivitiActivityEvent)event;
			logger.debug("\tactivityId = " + activityEvent.getActivityId());
			logger.debug("\tactivityName = " + activityEvent.getActivityName());
			logger.debug("\tactivityType = " + activityEvent.getActivityType());
			break;
		case PROCESS_COMPLETED:
			logger.debug("Event: PROCESS_COMPLETED");
			break;
		default:
			break;
		}
	}

	@Override
	public boolean isFailOnException() {
		return false;
	}

	/**
	 * 当流程结束时，如果有上层机构，是否自动启动上层机构的处理流程
	 */
	public boolean isStartUpperOnFinish() {
		return startUpperOnFinish;
	}

	/**
	 * 当流程结束时，如果有上层机构，是否自动启动上层机构的处理流程
	 */
	public void setStartUpperOnFinish(boolean startUpperOnFinish) {
		this.startUpperOnFinish = startUpperOnFinish;
	}

	public static ActivitiGlobalEventListener getInstance() {
		return instance;
	}
}
