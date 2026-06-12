export type CurrentUser = {
  username: string;
  email: string;
  roles: string[];
};

export type Project = {
  id: string;
  name: string;
  description: string | null;
  ownerUserId: string;
  ownerEmail: string;
  createdAt: string;
  updatedAt: string;
};

export type DocumentMetadata = {
  id: string;
  projectId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedByUserId: string;
  uploadedByEmail: string;
  createdAt: string;
};

export type AuditLog = {
  id: string;
  timestamp: string;
  actorUserId: string;
  actorEmail: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  result: string;
  ipAddress: string;
  userAgent: string;
  correlationId: string;
};
