/** Maps DB `ui_variant` to Chart.js colour from theme CSS variables. */
export function chartColorForUiVariant(
  uiVariant: string | null | undefined,
  colors: {
    success: string;
    danger: string;
    warning: string;
    info: string;
    secondary: string;
    neutral: string;
    primary: string;
  }
): string {
  switch ((uiVariant ?? 'neutral').toLowerCase().trim()) {
    case 'success':
      return colors.success;
    case 'danger':
      return colors.danger;
    case 'warning':
      return colors.warning;
    case 'info':
      return colors.info;
    case 'secondary':
      return colors.secondary;
    case 'primary':
      return colors.primary;
    default:
      return colors.neutral;
  }
}

export function chartThemeColors(): {
  success: string;
  danger: string;
  warning: string;
  info: string;
  secondary: string;
  neutral: string;
  primary: string;
  text: string;
  grid: string;
  surface: string;
} {
  const styles = getComputedStyle(document.documentElement);
  return {
    success: styles.getPropertyValue('--success-color').trim() || '#10B981',
    danger: styles.getPropertyValue('--error-color').trim() || '#eb0808',
    warning: styles.getPropertyValue('--warning-color').trim() || '#F59E0B',
    info: styles.getPropertyValue('--info-color').trim() || '#2563eb',
    secondary: styles.getPropertyValue('--text-secondary').trim() || '#64748b',
    neutral: styles.getPropertyValue('--text-subtle').trim() || '#94a3b8',
    primary: styles.getPropertyValue('--primary-color').trim() || '#0b6e4f',
    text: styles.getPropertyValue('--text-secondary').trim() || '#475569',
    grid: styles.getPropertyValue('--border-color').trim() || '#d9e3ef',
    surface: styles.getPropertyValue('--surface-elevated').trim() || '#ffffff'
  };
}
