"use client"

import { useCallback } from "react"
import { useDropzone } from "react-dropzone"
import { toast } from "sonner"
import { Upload, Loader2 } from "lucide-react"
import { useUploadDocument } from "@/hooks/use-documents"
import { cn } from "@/lib/utils"

export function DocumentUpload() {
  const uploadMutation = useUploadDocument()

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      for (const file of acceptedFiles) {
        const formData = new FormData()
        formData.append("file", file)
        formData.append("title", file.name.replace(/\.[^.]+$/, ""))
        formData.append("category", "OTHER")

        uploadMutation.mutate(formData, {
          onSuccess: () => {
            toast.success("Document uploaded")
          },
          onError: () => {
            toast.error(
              "Upload failed. Check the file format and try again.",
            )
          },
        })
      }
    },
    [uploadMutation],
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/pdf": [".pdf"],
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
        [".docx"],
    },
  })

  return (
    <div
      {...getRootProps()}
      className={cn(
        "border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors",
        isDragActive
          ? "border-primary bg-primary/5"
          : "border-muted-foreground/25 hover:border-muted-foreground/50",
      )}
    >
      <input {...getInputProps()} />

      {uploadMutation.isPending ? (
        <div className="flex items-center justify-center gap-2 text-muted-foreground">
          <Loader2 className="size-5 animate-spin" />
          <span className="text-sm">Uploading...</span>
        </div>
      ) : isDragActive ? (
        <div className="flex flex-col items-center gap-2">
          <Upload className="size-8 text-primary" />
          <p className="text-sm font-medium text-primary">Drop files here</p>
        </div>
      ) : (
        <div className="flex flex-col items-center gap-2">
          <Upload className="size-8 text-muted-foreground" />
          <p className="text-sm font-medium">
            Drag and drop files here, or click to browse
          </p>
          <p className="text-xs text-muted-foreground">
            PDF and DOCX files only
          </p>
        </div>
      )}
    </div>
  )
}
