"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertDialog,
  Button,
  Description,
  FieldError,
  Form,
  Input,
  Label,
  NumberField,
  Tag,
  TagGroup,
  TextField,
  toast,
} from "@heroui/react";
import { useState } from "react";
import { Controller, useFieldArray, useForm, type FieldPath, type Resolver } from "react-hook-form";
import { summaryFromFormErrors } from "@/features/auth/formErrors";
import { fieldErrors, isApiError } from "@/shared/api/apiError";
import type { CollectedPokemon } from "@/shared/api/types";
import { formatName } from "@/shared/lib/formatName";
import { problemCopy } from "@/shared/lib/problemCopy";
import { useReportUnsavedChanges } from "@/shared/lib/unsavedChanges";
import { ErrorSummary, type SummaryItem } from "@/shared/ui/ErrorSummary";
import { FormTextField } from "@/shared/ui/FormTextField";
import { useUpdateCollectedMutation } from "../collectionApi";
import {
  editSchema,
  fieldId,
  fromItem,
  mapServerField,
  MAX_TAGS,
  normalizeTag,
  TAG_PATTERN,
  toRequest,
  type EditValues,
} from "../lib/editForm";

type Props = {
  item: CollectedPokemon;
  onDone: () => void;
  onCancel: () => void;
  /** Refetch the record and re-open the form with the latest version (409 "Reload latest"). */
  onReloadLatest: () => Promise<void>;
  /** The record is gone (404 on save). */
  onRemoved: () => void;
};

/**
 * US04 edit form (ui-ux.md §5.4): localized names, geo, tags. Only custom attributes are
 * editable (BR-06); `version` goes with the request for optimistic concurrency (BR-08).
 */
export function EditForm({ item, onDone, onCancel, onReloadLatest, onRemoved }: Props) {
  const [update, { isLoading }] = useUpdateCollectedMutation();
  const [submitCount, setSubmitCount] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);
  const [conflictOpen, setConflictOpen] = useState(false);
  const [reloading, setReloading] = useState(false);
  const name = formatName(item.snapshot.name);

  const { control, handleSubmit, setError, formState } = useForm<EditValues>({
    resolver: zodResolver(editSchema) as unknown as Resolver<EditValues>,
    shouldFocusError: false, // the ErrorSummary takes focus instead
    defaultValues: fromItem(item.custom),
  });
  const names = useFieldArray({ control, name: "localizedNames" });
  // Lets the session-expiry handling keep this page (and the input) instead of redirecting.
  useReportUnsavedChanges(formState.isDirty);

  const onValid = async (values: EditValues) => {
    setFormError(null);
    try {
      await update({ pokedexId: item.id, body: toRequest(values, item.version) }).unwrap();
      toast.success("Saved", { description: `${name}’s custom fields were updated.` });
      onDone();
    } catch (error) {
      if (!isApiError(error)) {
        setFormError(problemCopy(undefined).message);
      } else if (error.status === 409) {
        setConflictOpen(true);
        return;
      } else if (error.status === 404) {
        onRemoved();
        return;
      } else {
        const violations = fieldErrors(error);
        let mapped = 0;
        for (const v of violations) {
          const path = mapServerField(v.field);
          if (path) {
            setError(path as FieldPath<EditValues>, { message: v.message });
            mapped += 1;
          }
        }
        if (mapped === 0) setFormError(problemCopy(error.problem).message);
      }
      setSubmitCount((n) => n + 1);
    }
  };

  const summary: SummaryItem[] = [
    ...(formError ? [{ message: formError }] : []),
    ...summaryFromFormErrors(formState.errors, fieldId),
  ];

  return (
    <>
      <Form
        className="flex flex-col gap-6"
        validationBehavior="aria"
        aria-labelledby="edit-heading"
        onSubmit={handleSubmit(onValid, () => setSubmitCount((n) => n + 1))}
      >
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 id="edit-heading" className="text-2xl font-semibold">
            Edit {name}
          </h2>
          <span className="text-sm text-muted">Version {item.version}</span>
        </div>

        <ErrorSummary items={summary} focusKey={submitCount} />

        <fieldset className="flex flex-col gap-3">
          <legend className="mb-2 text-lg font-semibold">Localized names</legend>
          {names.fields.length === 0 && <p className="text-sm text-muted">No localized names yet.</p>}
          <ul className="flex flex-col gap-3">
            {names.fields.map((row, index) => (
              <li key={row.id} className="flex flex-wrap items-start gap-2">
                <FormTextField
                  control={control}
                  name={`localizedNames.${index}.lang`}
                  id={fieldId(`localizedNames.${index}.lang`)}
                  label="Language"
                  placeholder="ja"
                  className="w-28"
                />
                <FormTextField
                  control={control}
                  name={`localizedNames.${index}.value`}
                  id={fieldId(`localizedNames.${index}.value`)}
                  label="Name"
                  className="min-w-48 flex-1"
                />
                <Button
                  isIconOnly
                  variant="ghost"
                  className="mt-7"
                  aria-label={`Remove localized name ${index + 1}`}
                  onPress={() => names.remove(index)}
                >
                  <TrashIcon />
                </Button>
              </li>
            ))}
          </ul>
          <div>
            <Button variant="secondary" size="sm" onPress={() => names.append({ lang: "", value: "" })}>
              Add language
            </Button>
          </div>
        </fieldset>

        <fieldset className="grid gap-4 sm:grid-cols-2">
          <legend className="mb-2 text-lg font-semibold">Where it lives</legend>
          <FormTextField control={control} name="region" id={fieldId("region")} label="Region" placeholder="Kanto" />
          <FormTextField control={control} name="habitat" id={fieldId("habitat")} label="Habitat" placeholder="Viridian Forest" />
          <CoordinateField control={control} name="latitude" label="Latitude" description="-90 to 90" />
          <CoordinateField control={control} name="longitude" label="Longitude" description="-180 to 180" />
        </fieldset>

        <Controller
          control={control}
          name="tags"
          render={({ field, fieldState }) => (
            <TagsField
              tags={field.value}
              onChange={field.onChange}
              error={fieldState.error?.message}
            />
          )}
        />

        <p className="rounded-xl bg-default p-3 text-sm text-muted">
          Name, sprite, stats and abilities come from PokeAPI and are read-only. Use “Refresh from PokeAPI” to update
          them.
        </p>

        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="tertiary" onPress={onCancel} isDisabled={isLoading}>
            Cancel
          </Button>
          <Button type="submit" isPending={isLoading} isDisabled={isLoading}>
            Save changes
          </Button>
        </div>
      </Form>

      <AlertDialog.Backdrop isOpen={conflictOpen} onOpenChange={setConflictOpen} isKeyboardDismissDisabled={false}>
        <AlertDialog.Container size="sm">
          <AlertDialog.Dialog>
            <AlertDialog.Header>
              <AlertDialog.Icon status="warning" />
              <AlertDialog.Heading>Someone else updated this record</AlertDialog.Heading>
            </AlertDialog.Header>
            <AlertDialog.Body>
              <p>
                {name} changed after you opened this form. Reload the latest version (your unsaved changes are
                discarded), or keep editing and copy what you need first.
              </p>
            </AlertDialog.Body>
            <AlertDialog.Footer>
              <Button variant="tertiary" slot="close">
                Keep editing
              </Button>
              <Button
                isPending={reloading}
                isDisabled={reloading}
                onPress={async () => {
                  setReloading(true);
                  try {
                    await onReloadLatest();
                  } finally {
                    setReloading(false);
                    setConflictOpen(false);
                  }
                }}
              >
                Reload latest
              </Button>
            </AlertDialog.Footer>
          </AlertDialog.Dialog>
        </AlertDialog.Container>
      </AlertDialog.Backdrop>
    </>
  );
}

function CoordinateField({
  control,
  name,
  label,
  description,
}: {
  control: ReturnType<typeof useForm<EditValues>>["control"];
  name: "latitude" | "longitude";
  label: string;
  description: string;
}) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <NumberField
          // No min/max here on purpose: clamping would hide the error; Zod reports out-of-range values.
          value={field.value ?? Number.NaN}
          onChange={(value) => field.onChange(value === undefined || Number.isNaN(value) ? undefined : value)}
          onBlur={field.onBlur}
          isInvalid={fieldState.invalid}
          validationBehavior="aria"
          variant="secondary"
          formatOptions={{ maximumFractionDigits: 6 }}
          name={name}
          fullWidth
        >
          <Label>{label}</Label>
          <NumberField.Group>
            <NumberField.Input id={fieldId(name)} ref={field.ref} />
          </NumberField.Group>
          <Description>{description}</Description>
          <FieldError>{fieldState.error?.message}</FieldError>
        </NumberField>
      )}
    />
  );
}

function TagsField({
  tags,
  onChange,
  error,
}: {
  tags: string[];
  onChange: (tags: string[]) => void;
  error?: string;
}) {
  const [draft, setDraft] = useState("");
  const normalized = normalizeTag(draft);
  const draftInvalid = normalized.length > 0 && !TAG_PATTERN.test(normalized);

  const add = () => {
    if (!normalized) return;
    if (!tags.includes(normalized)) onChange([...tags, normalized]);
    setDraft("");
  };

  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-lg font-semibold">Tags</legend>
      {tags.length > 0 ? (
        <TagGroup
          aria-label="Tags"
          onRemove={(keys) => onChange(tags.filter((t) => !keys.has(t)))}
        >
          <TagGroup.List>
            {tags.map((tag) => (
              <Tag key={tag} id={tag} textValue={tag}>
                {tag}
                <Tag.RemoveButton aria-label={`Remove tag ${tag}`} />
              </Tag>
            ))}
          </TagGroup.List>
        </TagGroup>
      ) : (
        <p className="text-sm text-muted">No tags yet.</p>
      )}
      <div className="flex flex-wrap items-start gap-2">
        <TextField
          value={draft}
          onChange={setDraft}
          isInvalid={Boolean(error) || draftInvalid}
          validationBehavior="aria"
          variant="secondary"
          className="min-w-56 flex-1"
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === ",") {
              event.preventDefault();
              add();
            }
          }}
        >
          <Label>Add a tag</Label>
          <Input id={fieldId("tags")} placeholder="e.g. starter" maxLength={40} />
          <Description>
            {normalized
              ? `Will be saved as “${normalized}”. ${tags.length}/${MAX_TAGS} tags.`
              : `Press Enter to add. Lowercase a–z, 0–9 and -. ${tags.length}/${MAX_TAGS} tags.`}
          </Description>
          <FieldError>
            {error ?? (draftInvalid ? "Tags may only contain a–z, 0–9 and - (up to 30 characters)" : undefined)}
          </FieldError>
        </TextField>
        <Button variant="secondary" className="mt-7" onPress={add} isDisabled={!normalized || tags.length >= MAX_TAGS}>
          Add tag
        </Button>
      </div>
    </fieldset>
  );
}

function TrashIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" />
    </svg>
  );
}
