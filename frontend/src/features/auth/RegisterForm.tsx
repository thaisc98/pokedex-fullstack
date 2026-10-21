"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Card, Form, toast } from "@heroui/react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm, type FieldPath } from "react-hook-form";
import { z } from "zod";
import { safeRedirect } from "@/shared/lib/safeRedirect";
import { AppLink } from "@/shared/ui/AppLink";
import { ErrorSummary, type SummaryItem } from "@/shared/ui/ErrorSummary";
import { FormTextField } from "@/shared/ui/FormTextField";
import { useRegisterMutation } from "./authApi";
import { authErrorMessage, serverFieldErrors, summaryFromFormErrors } from "./formErrors";

/** Mirrors RegisterRequest (BR-09); the server stays the source of truth (common passwords, duplicates). */
const schema = z.object({
  displayName: z.string().trim().min(1, "Enter a display name").max(60, "Display name must be at most 60 characters"),
  email: z.string().trim().min(1, "Enter your email address").max(254).pipe(z.email("Enter a valid email address")),
  password: z
    .string()
    .min(12, "Password must be at least 12 characters")
    .max(64, "Password must be at most 64 characters"),
});
type Values = z.infer<typeof schema>;

const FIELD_IDS: Record<string, string> = {
  displayName: "register-display-name",
  email: "register-email",
  password: "register-password",
};

/** Register (ui-ux.md §5.5): password rules shown up front; on success, go to Log in. */
export function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const from = safeRedirect(searchParams.get("from"), "");
  const [register, { isLoading }] = useRegisterMutation();
  const [formError, setFormError] = useState<string | null>(null);
  const [submitCount, setSubmitCount] = useState(0);

  const { control, handleSubmit, setError, formState } = useForm<Values>({
    resolver: zodResolver(schema),
    // The ErrorSummary takes focus on a failed submit (ui-ux.md §4), not the first field.
    shouldFocusError: false,
    defaultValues: { displayName: "", email: "", password: "" },
  });

  const onValid = async (values: Values) => {
    setFormError(null);
    try {
      await register(values).unwrap();
      toast.success("Account created", { description: "Log in to start your collection." });
      router.push(from ? `/login?from=${encodeURIComponent(from)}` : "/login");
    } catch (error) {
      const fields = serverFieldErrors(error);
      for (const { field, message } of fields) {
        if (field in FIELD_IDS) setError(field as FieldPath<Values>, { message });
      }
      if (fields.length === 0) setFormError(authErrorMessage(error));
      setSubmitCount((n) => n + 1);
    }
  };

  const summary: SummaryItem[] = [
    ...(formError ? [{ message: formError }] : []),
    ...summaryFromFormErrors(formState.errors, (path) => FIELD_IDS[path] ?? path),
  ];

  return (
    <div className="mx-auto flex w-full max-w-sm flex-col gap-6 py-4">
      <h1 className="text-3xl font-bold tracking-tight">Create an account</h1>
      <Card>
        <Card.Content>
          <Form
            className="flex flex-col gap-4"
            validationBehavior="aria"
            onSubmit={handleSubmit(onValid, () => setSubmitCount((n) => n + 1))}
          >
            <ErrorSummary items={summary} focusKey={submitCount} />
            <FormTextField
              control={control}
              name="displayName"
              id={FIELD_IDS.displayName}
              label="Display name"
              autoComplete="nickname"
              isRequired
            />
            <FormTextField
              control={control}
              name="email"
              id={FIELD_IDS.email}
              label="Email"
              type="email"
              autoComplete="email"
              isRequired
            />
            <FormTextField
              control={control}
              name="password"
              id={FIELD_IDS.password}
              label="Password"
              type="password"
              autoComplete="new-password"
              description="12–64 characters. Any characters are fine; very common passwords are rejected."
              isRequired
            />
            <Button type="submit" fullWidth isPending={isLoading} isDisabled={isLoading}>
              Create account
            </Button>
          </Form>
        </Card.Content>
      </Card>
      <p className="text-center text-sm text-muted">
        Already have an account? <AppLink href="/login">Log in</AppLink>
      </p>
    </div>
  );
}
