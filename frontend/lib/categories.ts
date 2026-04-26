import type { DocumentCategoryOptionResponse } from "@/types/api";

export function formatCategoryLabel(
  code: string | null | undefined,
  options: DocumentCategoryOptionResponse[],
) {
  if (!code) {
    return "未分类";
  }

  const option = options.find((item) => item.code === code);
  return option ? `${option.displayName} (${option.code})` : code;
}

export function toCategorySelectOptions(options: DocumentCategoryOptionResponse[]) {
  return options.map((option) => ({
    value: option.code,
    label: `${option.displayName} (${option.code})`,
  }));
}
