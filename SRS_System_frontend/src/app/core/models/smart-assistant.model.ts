export type SmartAssistantRole = 'assistant' | 'user';

export interface SmartAssistantAction {
  id: string;
  label: string;
  route?: string;
  prompt?: string;
}

export interface SmartAssistantReply {
  text: string;
  actions?: SmartAssistantAction[];
}

export interface SmartAssistantMessage {
  id: string;
  role: SmartAssistantRole;
  text: string;
  createdAt: string;
  actions?: SmartAssistantAction[];
  pending?: boolean;
  error?: boolean;
}

