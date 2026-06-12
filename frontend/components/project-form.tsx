"use client";

import { FormEvent, useState } from "react";

import { Message } from "@/components/message";

type ProjectFormProps = {
  initialName?: string;
  initialDescription?: string;
  submitLabel: string;
  onSubmit: (value: { name: string; description: string }) => Promise<void>;
};

export function ProjectForm({
  initialName = "",
  initialDescription = "",
  submitLabel,
  onSubmit,
}: ProjectFormProps) {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ name: name.trim(), description: description.trim() });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not save project");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form card" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="project-name">Name</label>
        <input
          id="project-name"
          maxLength={100}
          onChange={(event) => setName(event.target.value)}
          required
          value={name}
        />
      </div>
      <div className="field">
        <label htmlFor="project-description">Description</label>
        <textarea
          id="project-description"
          maxLength={1000}
          onChange={(event) => setDescription(event.target.value)}
          value={description}
        />
      </div>
      <Message>{error}</Message>
      <div>
        <button className="button" disabled={submitting} type="submit">
          {submitting ? "Saving..." : submitLabel}
        </button>
      </div>
    </form>
  );
}
