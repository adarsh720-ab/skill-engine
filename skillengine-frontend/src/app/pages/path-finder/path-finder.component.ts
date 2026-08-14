import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, startWith, switchMap } from 'rxjs';
import { GraphData, GraphNode, SkillNodeDto, SkillPathResponse } from '../../core/models/skill.model';
import { SkillService } from '../../core/services/skill.service';
import { GraphCanvasComponent } from '../explorer/graph-canvas.component';

@Component({
  selector: 'app-path-finder',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, GraphCanvasComponent],
  templateUrl: './path-finder.component.html',
  styleUrl: './path-finder.component.scss',
})
export class PathFinderComponent {
  private readonly fb = inject(FormBuilder);
  private readonly skillService = inject(SkillService);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly paths = signal<SkillPathResponse[]>([]);
  readonly selectedPathIndex = signal(0);

  readonly startSuggestions = signal<SkillNodeDto[]>([]);
  readonly endSuggestions = signal<SkillNodeDto[]>([]);
  readonly showStartSuggestions = signal(false);
  readonly showEndSuggestions = signal(false);

  readonly form = this.fb.nonNullable.group({
    startSkill: ['', Validators.required],
    endSkill: ['', Validators.required],
    maxHops: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
  });

  readonly hasSearched = computed(() => this.paths().length > 0 || this.errorMessage() !== null);

  readonly selectedPath = computed<SkillPathResponse | null>(() => {
    const list = this.paths();
    return list.length > 0 ? list[this.selectedPathIndex()] ?? list[0] : null;
  });

  readonly graphData = computed<GraphData>(() => {
    const path = this.selectedPath();
    if (!path) return { nodes: [], links: [] };

    const nodes: GraphNode[] = path.path.map((n, i) => ({
      id: n.name,
      category: n.category,
      isRoot: i === 0,
      isEndpoint: i === path.path.length - 1,
    }));

    const links = [];
    for (let i = 0; i < path.path.length - 1; i++) {
      links.push({ source: path.path[i].name, target: path.path[i + 1].name });
    }

    return { nodes, links };
  });

  constructor() {
    // Empty query on load fetches the entry-point skills, so both dropdowns
    // have something browsable before the user types anything.
    this.form.controls.startSkill.valueChanges
      .pipe(
        startWith(''),
        debounceTime(200),
        distinctUntilChanged(),
        switchMap((query) => this.skillService.searchSkills(query, 8)),
        takeUntilDestroyed(),
      )
      .subscribe((results) => this.startSuggestions.set(results));

    this.form.controls.endSkill.valueChanges
      .pipe(
        startWith(''),
        debounceTime(200),
        distinctUntilChanged(),
        switchMap((query) => this.skillService.searchSkills(query, 8)),
        takeUntilDestroyed(),
      )
      .subscribe((results) => this.endSuggestions.set(results));
  }

  pickStart(name: string): void {
    this.form.controls.startSkill.setValue(name);
    this.showStartSuggestions.set(false);
  }

  pickEnd(name: string): void {
    this.form.controls.endSkill.setValue(name);
    this.showEndSuggestions.set(false);
  }

  search(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.selectedPathIndex.set(0);

    this.skillService.findPaths(this.form.getRawValue()).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.paths.set(res);
        if (res.length === 0) {
          this.errorMessage.set('No path found between those skills within the hop limit.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.paths.set([]);
        if (err.status === 0) {
          this.errorMessage.set('Cannot reach the SkillEngine API. Is the backend running?');
        } else if (err.error?.message) {
          this.errorMessage.set(err.error.message);
        } else {
          this.errorMessage.set('Something went wrong while finding a path.');
        }
      },
    });
  }

  selectPath(index: number): void {
    this.selectedPathIndex.set(index);
  }
}