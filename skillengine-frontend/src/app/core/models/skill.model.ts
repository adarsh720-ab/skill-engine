// Mirrors com.wexa.skillengine.dto.response.SkillNodeDto
export interface SkillNodeDto {
  name: string;
  category: string;
}

export interface SkillEdgeDto {
  source: string;
  target: string;
}


// Mirrors com.wexa.skillengine.dto.response.SkillStackResponse
export interface SkillStackResponse {
  rootSkill: string;
  rootCategory: string;
  relatedSkills: SkillNodeDto[];
  edges: SkillEdgeDto[];
}

// Mirrors com.wexa.skillengine.dto.response.SkillPathResponse
export interface SkillPathResponse {
  path: SkillNodeDto[];
  hopCount: number;
}

// Mirrors com.wexa.skillengine.dto.request.SkillPathRequest
export interface SkillPathRequest {
  startSkill: string;
  endSkill: string;
  maxHops: number;
}

// Mirrors com.wexa.skillengine.dto.request.CreateSkillRequest
export interface CreateSkillRequest {
  name: string;
  category: string;
  prerequisiteFor?: string[];
}

// Mirrors com.wexa.skillengine.dto.response.AuthResponse
export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role?: string;
}

// Graph rendering primitives used by the D3 canvas
export interface GraphNode {
  id: string; // skill name, unique key
  category: string;
  isRoot?: boolean;
  isEndpoint?: boolean;
  x?: number;
  y?: number;
  fx?: number | null;
  fy?: number | null;
}

export interface GraphLink {
  source: string;
  target: string;
}

export interface GraphData {
  nodes: GraphNode[];
  links: GraphLink[];
}
