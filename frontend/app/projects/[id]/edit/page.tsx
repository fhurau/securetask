import { EditProject } from "@/components/edit-project";

export default async function EditProjectPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <EditProject projectId={id} />;
}
