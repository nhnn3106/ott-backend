class ContentReviewRequest {
  constructor({
    requestId,
    sourceService,
    eventType,
    contentType,
    contentRefId,
    userId,
    tenantId,
    payload,
    metadata,
    createdAt,
  }) {
    this.requestId = requestId;
    this.sourceService = sourceService;
    this.eventType = eventType;
    this.contentType = contentType;
    this.contentRefId = contentRefId;
    this.userId = userId;
    this.tenantId = tenantId;
    this.payload = payload || {};
    this.metadata = metadata || {};
    this.createdAt = createdAt || new Date().toISOString();
  }

  static build(values) {
    return new ContentReviewRequest(values);
  }
}

module.exports = ContentReviewRequest;
