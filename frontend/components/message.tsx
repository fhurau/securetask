export function Message({
  children,
  kind = "error",
}: {
  children: string | null;
  kind?: "error" | "success";
}) {
  if (!children) return null;
  return <p className={kind}>{children}</p>;
}
