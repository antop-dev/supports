(function (global) {
    'use strict';

    // [data-filepond] 파일 입력을 FilePond로 초기화한다.
    // 청크 업로드(FeedbackUploadController)로 선택 즉시 백그라운드 전송한다.
    // chunkForce: true 로 모든 파일(작은 파일 포함)을 같은 방식으로 보내 서버 로직을 단순하게 유지한다.
    // 서버는 app.upload.chunk-size-bytes(기본 8MB) 이하 청크만 허용하므로 여유 있게 그보다 작게 잡는다.
    var CHUNK_SIZE = 5 * 1024 * 1024;
    var ponds = [];

    document.addEventListener('DOMContentLoaded', function () {
        if (typeof FilePond === 'undefined') {
            return;
        }

        document.querySelectorAll('input[type="file"][data-filepond]').forEach(function (input) {
            var maxFiles = parseInt(input.getAttribute('data-max-files'), 10);
            var maxFileSize = parseInt(input.getAttribute('data-max-file-size'), 10);
            var uploadUrl = input.getAttribute('data-upload-url');

            var options = {
                name: input.name || 'files',
                allowMultiple: true,
                maxFiles: isNaN(maxFiles) ? null : maxFiles,
                credits: false,
                labelIdle: '파일을 끌어다 놓거나 <span class="filepond--label-action">찾아보기</span>',
                labelFileWaitingForSize: '크기 계산 중',
                labelFileLoadError: '불러오기 실패',
                labelMaxFileSizeExceeded: '파일이 너무 큽니다',
                labelFileTypeNotAllowed: '허용되지 않는 파일 형식입니다',
                labelFileProcessingError: '업로드 실패',
                labelFileProcessingAborted: '업로드가 취소되었습니다'
            };
            if (!isNaN(maxFileSize)) {
                options.maxFileSize = maxFileSize;
            }
            if (uploadUrl) {
                options.chunkUploads = true;
                options.chunkForce = true;
                options.chunkSize = CHUNK_SIZE;
                // FilePond는 청크 PATCH 주소를 process 에서 자동으로 유추하지 않는다.
                // patch 를 문자열로 주면 그 문자열 뒤에 업로드 id를 그대로 이어붙여서 보낸다
                // (기본값이 "?patch=" 뒤에 id를 붙이는 것과 같은 방식) — 그래서 끝에 "/" 를 붙여
                // "{uploadUrl}/{id}" 형태(PatchMapping("/{id}")와 일치)로 만든다.
                // revert(DELETE)는 uploadUrl 그대로, id는 본문에 담아 보낸다.
                // server.headers 는 process/patch/revert 요청 모두에 함께 실린다(청크 PATCH 포함).
                options.server = {
                    headers: window.Csrf.headers(),
                    process: uploadUrl,
                    patch: uploadUrl + '/',
                    revert: uploadUrl
                };
            }

            var pond = FilePond.create(input, options);
            ponds.push(pond);
        });
    });

    global.FilePondInit = {
        /** 업로드 중이거나 업로드 대기 중인 파일이 하나라도 있으면 true. */
        isBusy: function () {
            if (typeof FilePond === 'undefined') {
                return false;
            }
            var busyStatuses = [
                FilePond.FileStatus.PROCESSING_QUEUED,
                FilePond.FileStatus.PROCESSING,
                FilePond.FileStatus.LOADING
            ];
            return ponds.some(function (pond) {
                return pond.getFiles().some(function (f) {
                    return busyStatuses.indexOf(f.status) !== -1;
                });
            });
        },
        /** 업로드에 실패해 더 진행할 수 없는 파일이 하나라도 있으면 true. */
        hasError: function () {
            if (typeof FilePond === 'undefined') {
                return false;
            }
            var errorStatuses = [FilePond.FileStatus.PROCESSING_ERROR, FilePond.FileStatus.LOAD_ERROR];
            return ponds.some(function (pond) {
                return pond.getFiles().some(function (f) {
                    return errorStatuses.indexOf(f.status) !== -1;
                });
            });
        }
    };
})(window);
