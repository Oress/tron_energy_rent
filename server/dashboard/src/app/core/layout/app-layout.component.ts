import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { versionInfo } from '../../../environments/version';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss',
})
export class AppLayoutComponent {
  readonly version = versionInfo.version;
  readonly commit = versionInfo.commit;
}
