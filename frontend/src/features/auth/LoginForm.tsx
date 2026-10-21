"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Card, Form } from "@heroui/react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm, type FieldPath } from "react-hook-form";
import { z } from "zod";
import { safeRedirect } from "@/shared/lib/safeRedirect";
import { AppLink } from "@/shared/ui/AppLink";
import { ErrorSummary, type SummaryItem } from "@/shared/ui/ErrorSummary";
import { FormTextField } from "@/shared/ui/FormTextField";
import { useLoginMutation } from "./authApi";
import { authErrorMessage, serverFieldErrors, summaryFromFormErrors } from "./formErrors";

const schema = z.object({
  email: z.string().trim().min(1, "Enter your email address").max(254).pipe(z.email("Enter a valid email address")),
  password: z.string().min(1, "Enter your password").max(64, "Password must be at most 64 characters"),
});
type Values = z.infer<typeof schema>;

const FIELD_IDS: Record<string, string> = { email: "login-email", password: "login-password" };

/** Log in (ui-ux.md §5.5). Bad credentials get one generic message (security.md §4.1). */
export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const from = safeRedirect(searchParams.get("from"));
  const [login, { isLoading }] = useLoginMutation();
  const [formError, setFormError] = useState<string | null>(null);
  const [submitCount, setSubmitCount] = useState(0);

  const { control, handleSubmit, setError, formState } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const onValid = async (values: Values) => {
    setFormError(null);
    try {
      await login(values).unwrap();
      router.replace(from);
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
      <h1 className="text-3xl font-bold tracking-tight">Log in</h1>
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
              autoComplete="current-password"
              isRequired
            />
            <Button type="submit" fullWidth isPending={isLoading} isDisabled={isLoading}>
              Log in
            </Button>
          </Form>
        </Card.Content>
      </Card>
      <p className="text-center text-sm text-muted">
        New here?{" "}
        <AppLink href={from !== "/collection" ? `/register?from=${encodeURIComponent(from)}` : "/register"}>
          Create an account
        </AppLink>
      </p>
    </div>
  );
}
