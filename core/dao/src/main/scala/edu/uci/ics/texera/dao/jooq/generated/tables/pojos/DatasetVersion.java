/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.dao.jooq.generated.tables.pojos;


import edu.uci.ics.texera.dao.jooq.generated.tables.interfaces.IDatasetVersion;

import java.sql.Timestamp;

import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasetVersion implements IDatasetVersion {

    private static final long serialVersionUID = 1052637628;

    private UInteger  dvid;
    private UInteger  did;
    private UInteger  creatorUid;
    private String    name;
    private String    versionHash;
    private Timestamp creationTime;

    public DatasetVersion() {}

    public DatasetVersion(IDatasetVersion value) {
        this.dvid = value.getDvid();
        this.did = value.getDid();
        this.creatorUid = value.getCreatorUid();
        this.name = value.getName();
        this.versionHash = value.getVersionHash();
        this.creationTime = value.getCreationTime();
    }

    public DatasetVersion(
        UInteger  dvid,
        UInteger  did,
        UInteger  creatorUid,
        String    name,
        String    versionHash,
        Timestamp creationTime
    ) {
        this.dvid = dvid;
        this.did = did;
        this.creatorUid = creatorUid;
        this.name = name;
        this.versionHash = versionHash;
        this.creationTime = creationTime;
    }

    @Override
    public UInteger getDvid() {
        return this.dvid;
    }

    @Override
    public void setDvid(UInteger dvid) {
        this.dvid = dvid;
    }

    @Override
    public UInteger getDid() {
        return this.did;
    }

    @Override
    public void setDid(UInteger did) {
        this.did = did;
    }

    @Override
    public UInteger getCreatorUid() {
        return this.creatorUid;
    }

    @Override
    public void setCreatorUid(UInteger creatorUid) {
        this.creatorUid = creatorUid;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVersionHash() {
        return this.versionHash;
    }

    @Override
    public void setVersionHash(String versionHash) {
        this.versionHash = versionHash;
    }

    @Override
    public Timestamp getCreationTime() {
        return this.creationTime;
    }

    @Override
    public void setCreationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatasetVersion (");

        sb.append(dvid);
        sb.append(", ").append(did);
        sb.append(", ").append(creatorUid);
        sb.append(", ").append(name);
        sb.append(", ").append(versionHash);
        sb.append(", ").append(creationTime);

        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IDatasetVersion from) {
        setDvid(from.getDvid());
        setDid(from.getDid());
        setCreatorUid(from.getCreatorUid());
        setName(from.getName());
        setVersionHash(from.getVersionHash());
        setCreationTime(from.getCreationTime());
    }

    @Override
    public <E extends IDatasetVersion> E into(E into) {
        into.from(this);
        return into;
    }
}
