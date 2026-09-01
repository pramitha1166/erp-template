package com.eudext.erp.workflow.internal.chain;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** WF-1 / WF-2 / WF-3: approval chain configuration — chains, their steps, and step conditions. */
@Service
public class ApprovalChainService {

    private final ApprovalChainRepository chainRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalStepConditionRepository conditionRepository;

    public ApprovalChainService(
            ApprovalChainRepository chainRepository,
            ApprovalStepRepository stepRepository,
            ApprovalStepConditionRepository conditionRepository) {
        this.chainRepository = chainRepository;
        this.stepRepository = stepRepository;
        this.conditionRepository = conditionRepository;
    }

    /** Deactivates whichever chain is currently active for (companyId, documentType), if any, before creating the new one. */
    @Transactional
    public ApprovalChain createChain(UUID tenantId, UUID companyId, String documentType, String name) {
        chainRepository
                .findByCompanyIdAndDocumentTypeAndActiveTrue(companyId, documentType)
                .ifPresent(ApprovalChain::deactivate);
        return chainRepository.save(ApprovalChain.create(tenantId, companyId, documentType, name));
    }

    @Transactional
    public void activate(UUID chainId) {
        ApprovalChain chain = getChain(chainId);
        chainRepository
                .findByCompanyIdAndDocumentTypeAndActiveTrue(chain.getCompanyId(), chain.getDocumentType())
                .ifPresent(ApprovalChain::deactivate);
        chain.activate();
        chainRepository.save(chain);
    }

    @Transactional
    public void deactivate(UUID chainId) {
        getChain(chainId).deactivate();
    }

    @Transactional
    public ApprovalStep addRoleStep(UUID tenantId, UUID chainId, int sequenceOrder, String name, UUID roleId) {
        return stepRepository.save(ApprovalStep.role(tenantId, chainId, sequenceOrder, name, roleId));
    }

    @Transactional
    public ApprovalStep addUserStep(UUID tenantId, UUID chainId, int sequenceOrder, String name, UUID userId) {
        return stepRepository.save(ApprovalStep.user(tenantId, chainId, sequenceOrder, name, userId));
    }

    @Transactional
    public ApprovalStep addHierarchyStep(UUID tenantId, UUID chainId, int sequenceOrder, String name, int level) {
        return stepRepository.save(ApprovalStep.hierarchy(tenantId, chainId, sequenceOrder, name, level));
    }

    @Transactional
    public void configureEscalation(
            UUID stepId, Integer hours, ApproverType type, UUID roleId, UUID userId, Integer hierarchyLevel) {
        ApprovalStep step = stepRepository.findById(stepId).orElseThrow(() -> new NoSuchElementException("No such step"));
        step.configureEscalation(hours, type, roleId, userId, hierarchyLevel);
        stepRepository.save(step);
    }

    @Transactional
    public ApprovalStepCondition addNumberCondition(
            UUID tenantId, UUID stepId, String fieldName, ConditionOperator operator, BigDecimal value) {
        return conditionRepository.save(ApprovalStepCondition.ofNumber(tenantId, stepId, fieldName, operator, value));
    }

    @Transactional
    public ApprovalStepCondition addTextCondition(
            UUID tenantId, UUID stepId, String fieldName, ConditionOperator operator, String value) {
        return conditionRepository.save(ApprovalStepCondition.ofText(tenantId, stepId, fieldName, operator, value));
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalChain> resolveActiveChain(UUID companyId, String documentType) {
        return chainRepository.findByCompanyIdAndDocumentTypeAndActiveTrue(companyId, documentType);
    }

    @Transactional(readOnly = true)
    public ApprovalChain getChain(UUID chainId) {
        return chainRepository.findById(chainId).orElseThrow(() -> new NoSuchElementException("No such approval chain"));
    }

    @Transactional(readOnly = true)
    public List<ApprovalChain> chainsFor(UUID companyId, String documentType) {
        return chainRepository.findByCompanyIdAndDocumentType(companyId, documentType);
    }

    @Transactional(readOnly = true)
    public List<ApprovalStep> stepsOf(UUID chainId) {
        return stepRepository.findByChainIdOrderBySequenceOrderAsc(chainId);
    }

    @Transactional(readOnly = true)
    public List<ApprovalStepCondition> conditionsOf(UUID stepId) {
        return conditionRepository.findByStepId(stepId);
    }
}
