/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.dataflow.jooq.generated.tables.daos;


import edu.uci.ics.texera.dataflow.jooq.generated.tables.Workflow;
import edu.uci.ics.texera.dataflow.jooq.generated.tables.records.WorkflowRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;
import org.jooq.types.UInteger;

import java.sql.Timestamp;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class WorkflowDao extends DAOImpl<WorkflowRecord, edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow, UInteger> {

    /**
     * Create a new WorkflowDao without any configuration
     */
    public WorkflowDao() {
        super(Workflow.WORKFLOW, edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow.class);
    }

    /**
     * Create a new WorkflowDao with an attached configuration
     */
    public WorkflowDao(Configuration configuration) {
        super(Workflow.WORKFLOW, edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow.class, configuration);
    }

    @Override
    public UInteger getId(edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow object) {
        return object.getWfId();
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Workflow.WORKFLOW.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchByName(String... values) {
        return fetch(Workflow.WORKFLOW.NAME, values);
    }

    /**
     * Fetch records that have <code>wf_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchRangeOfWfId(UInteger lowerInclusive, UInteger upperInclusive) {
        return fetchRange(Workflow.WORKFLOW.WF_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>wf_id IN (values)</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchByWfId(UInteger... values) {
        return fetch(Workflow.WORKFLOW.WF_ID, values);
    }

    /**
     * Fetch a unique record that has <code>wf_id = value</code>
     */
    public edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow fetchOneByWfId(UInteger value) {
        return fetchOne(Workflow.WORKFLOW.WF_ID, value);
    }

    /**
     * Fetch records that have <code>content BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchRangeOfContent(String lowerInclusive, String upperInclusive) {
        return fetchRange(Workflow.WORKFLOW.CONTENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>content IN (values)</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchByContent(String... values) {
        return fetch(Workflow.WORKFLOW.CONTENT, values);
    }

    /**
     * Fetch records that have <code>creation_time BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchRangeOfCreationTime(Timestamp lowerInclusive, Timestamp upperInclusive) {
        return fetchRange(Workflow.WORKFLOW.CREATION_TIME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>creation_time IN (values)</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchByCreationTime(Timestamp... values) {
        return fetch(Workflow.WORKFLOW.CREATION_TIME, values);
    }

    /**
     * Fetch records that have <code>last_modified_time BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchRangeOfLastModifiedTime(Timestamp lowerInclusive, Timestamp upperInclusive) {
        return fetchRange(Workflow.WORKFLOW.LAST_MODIFIED_TIME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>last_modified_time IN (values)</code>
     */
    public List<edu.uci.ics.texera.dataflow.jooq.generated.tables.pojos.Workflow> fetchByLastModifiedTime(Timestamp... values) {
        return fetch(Workflow.WORKFLOW.LAST_MODIFIED_TIME, values);
    }
}
