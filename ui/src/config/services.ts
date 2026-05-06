/**
 * 后端服务配置
 * 
 * 集中管理所有后端服务的路径前缀，便于维护和扩展
 */

export const SERVICES = {
  BIZ: '/biz',      // 业务服务 (jnet-biz) - 端口 9203
  ANNO: '/anno',    // 标注服务 (jnet-anno) - 端口 9005
  // 未来可以添加更多服务
  // AUTH: '/auth',         // 认证服务
  // ANALYSIS: '/analysis', // AI分析服务
} as const

export type ServiceKey = keyof typeof SERVICES
export type ServicePrefix = typeof SERVICES[ServiceKey]

/**
 * 生成完整API路径
 * @param servicePrefix 服务前缀（如 SERVICES.BIZ 的值 '/biz'）
 * @param path API路径
 * @returns 完整的API路径
 * 
 * @example
 * createApiPath(SERVICES.BIZ, '/api/v1/images')
 * // 返回: '/biz/api/v1/images'
 */
export function createApiPath(servicePrefix: ServicePrefix, path: string): string {
  return `${servicePrefix}${path}`
}

/**
 * 获取服务前缀
 * @param service 服务键
 * @returns 服务前缀字符串
 */
export function getServicePrefix(service: ServiceKey): string {
  return SERVICES[service]
}
