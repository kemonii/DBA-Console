/** An Oracle directory object. Mirrors `una.dbadmin.files.DirectoryObject`. */
export interface DirectoryObject {
  name: string;
  path: string;
}

/** A `.dmp` or `.log` file inside a directory. Mirrors `una.dbadmin.files.BackupFile`. */
export interface BackupFile {
  name: string;
  sizeBytes: number;
  /** ISO-8601 timestamp. */
  lastModified: string;
}

/** The text of a log file. Mirrors `una.dbadmin.files.LogContent`. */
export interface LogContent {
  name: string;
  content: string;
  /** True when the file has more content than was returned. */
  truncated: boolean;
}
