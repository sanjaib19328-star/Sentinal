import { ApiEndpoint } from './application';

export type DocumentationStatus = 'DISCOVERED' | 'DOCUMENTED' | 'DOCUMENTED_AND_DISCOVERED';

export interface OpenApiImportRequest {
  specContent?: string;
  specUrl?: string;
}

export interface OpenApiImportResponse {
  applicationId: number;
  endpointsImported: number;
  endpointsUpdated?: number;
  totalDocumentedEndpoints: number;
  schemasCount?: number;
  parametersCount?: number;
  requestBodiesCount?: number;
  transitionsCount?: number;
  endpoints: ApiEndpoint[];
}
