
import { AlertCircle, ArrowLeft, Loader2, RotateCcw } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Progress } from "@/components/ui/progress"

import {
  getRepoProgress,
  useStartIndexing,
} from "@/hooks/use-repos"

export function IndexingState({ repo, status }) {
  const indexMutation = useStartIndexing()

  const filesProcessed =
    status?.filesProcessed ?? repo.filesProcessed

  const filesTotal =
    status?.filesTotal ?? repo.filesTotal

  const chunkCount =
    status?.chunkCount ?? repo.chunkCount

  const progress = getRepoProgress({
    filesProcessed,
    filesTotal,
  })

  const indexStatus =
    status?.indexStatus ?? repo.indexStatus

  const errorMessage =
    status?.errorMessage ?? repo.errorMessage

  const navigate = useNavigate()
  const isTrademarkBlocked =
    errorMessage?.includes("trademark") ||
    errorMessage?.includes("Repository access blocked") ||
    errorMessage?.includes("disabled by GitHub")

  if (indexStatus === "FAILED") {
    return (
      <Empty className="h-full border-0">
        <EmptyHeader>
          <EmptyMedia variant="icon">
            <AlertCircle className="text-destructive" />
          </EmptyMedia>

          <EmptyTitle>
            {isTrademarkBlocked ? "Repository Disabled by GitHub" : "Indexing failed"}
          </EmptyTitle>

          <EmptyDescription className="max-w-md">
            {isTrademarkBlocked
              ? "Access to this repository was disabled by GitHub Staff due to a Trademark Policy violation. Please select an active repository from your dashboard."
              : errorMessage || "Something went wrong while indexing this repository."}
          </EmptyDescription>
        </EmptyHeader>

        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => navigate("/dashboard")}>
            <ArrowLeft data-icon="inline-start" />
            Back to Repositories
          </Button>

          {!isTrademarkBlocked && (
            <Button
              onClick={() => indexMutation.mutate(repo.id)}
              disabled={indexMutation.isPending}
            >
              <RotateCcw data-icon="inline-start" />
              Retry indexing
            </Button>
          )}
        </div>
      </Empty>
    )
  }

  return (
    <Empty className="h-full border-0">
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <Loader2 className="animate-spin" />
        </EmptyMedia>

        <EmptyTitle>
          Indexing {repo.fullName}
        </EmptyTitle>

        <EmptyDescription>
          {filesTotal > 0
            ? `${filesProcessed} of ${filesTotal} files · ${chunkCount} chunks embedded`
            : "Fetching repository files and preparing embeddings…"}
        </EmptyDescription>
      </EmptyHeader>

      <div className="w-full max-w-sm space-y-2">
        <Progress
          value={Math.max(
            progress,
            filesTotal ? progress : 12
          )}
        />

        <p className="text-center text-xs text-muted-foreground">
          You can leave this page open — chat unlocks when indexing finishes.
        </p>
      </div>
    </Empty>
  )
}
