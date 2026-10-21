"use client";

import { Description, FieldError, Input, Label, TextField } from "@heroui/react";
import type { ComponentProps } from "react";
import { Controller, type Control, type FieldPath, type FieldValues } from "react-hook-form";

type Props<T extends FieldValues> = {
  control: Control<T>;
  name: FieldPath<T>;
  label: string;
  /** DOM id of the input, so the ErrorSummary can link to it. */
  id: string;
  description?: string;
  type?: "text" | "email" | "password";
  autoComplete?: string;
  isRequired?: boolean;
  placeholder?: string;
  className?: string;
  inputMode?: ComponentProps<typeof Input>["inputMode"];
};

/** HeroUI TextField wired to React Hook Form: label, description and the field's error. */
export function FormTextField<T extends FieldValues>({
  control,
  name,
  label,
  id,
  description,
  type = "text",
  autoComplete,
  isRequired,
  placeholder,
  className,
  inputMode,
}: Props<T>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <TextField
          className={className}
          name={field.name}
          type={type}
          value={(field.value as string | undefined) ?? ""}
          onChange={field.onChange}
          onBlur={field.onBlur}
          isInvalid={fieldState.invalid}
          isRequired={isRequired}
          validationBehavior="aria"
          // Our forms sit on Card/Surface; the primary field color matches the surface in dark mode.
          variant="secondary"
          fullWidth
        >
          <Label>{label}</Label>
          <Input
            id={id}
            ref={field.ref}
            autoComplete={autoComplete}
            placeholder={placeholder}
            inputMode={inputMode}
          />
          {description && <Description>{description}</Description>}
          <FieldError>{fieldState.error?.message}</FieldError>
        </TextField>
      )}
    />
  );
}
