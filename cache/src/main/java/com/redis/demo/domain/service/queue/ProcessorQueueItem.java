package com.redis.demo.domain.service.queue;

public class ProcessorQueueItem {
    private long id;
    private long companyId;
    private long priority;

    public ProcessorQueueItem() {
    }

    public ProcessorQueueItem(long id, long companyId, long priority) {
        this.id = id;
        this.companyId = companyId;
        this.priority = priority;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }
}
