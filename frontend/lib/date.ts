import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";

dayjs.extend(relativeTime);

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }

  return dayjs(value).format("YYYY-MM-DD HH:mm:ss");
}

export function formatRelative(value?: string | null) {
  if (!value) {
    return "-";
  }

  return dayjs(value).fromNow();
}

export function formatDurationMs(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }

  if (value < 1000) {
    return `${Math.round(value)} ms`;
  }

  const seconds = value / 1000;
  if (seconds < 60) {
    return `${seconds.toFixed(1)} s`;
  }

  const minutes = seconds / 60;
  return `${minutes.toFixed(1)} min`;
}

export function diffHours(start?: string | null, end?: string | null) {
  if (!start || !end) {
    return null;
  }

  const diffMs = dayjs(end).diff(dayjs(start), "millisecond");
  return diffMs >= 0 ? diffMs / 1000 / 60 / 60 : null;
}

export function formatHours(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }

  if (value < 1) {
    return `${Math.round(value * 60)} min`;
  }

  return `${value.toFixed(1)} h`;
}

